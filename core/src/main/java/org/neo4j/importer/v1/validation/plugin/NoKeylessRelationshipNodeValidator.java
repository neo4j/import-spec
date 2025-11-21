/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.importer.v1.validation.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.util.collections.Sets;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoKeylessRelationshipNodeValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "NKRN-001";

    private final Map<String, NodeShape> visitedNodeShapes = new HashMap<>();

    private final Set<String> targetsWithoutConstraint = new HashSet<>();

    private final Set<NodePattern> nodePatternsWithConstraints = new HashSet<>();

    private final Map<String, String> errorMessages = new LinkedHashMap<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDuplicatedNameValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class,
                NoDanglingPropertyInUniqueConstraintValidator.class,
                NoDanglingNodeReferenceValidator.class,
                NoDanglingPropertyInNodeReferenceKeyMappingsValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        visitedNodeShapes.put(target.getName(), new NodeShape(target));
        var schema = target.getSchema();
        var keyConstraints = schema.getKeyConstraints();
        var uniqueConstraints = schema.getUniqueConstraints();
        if (keyConstraints.isEmpty() && uniqueConstraints.isEmpty()) {
            targetsWithoutConstraint.add(target.getName());
        } else {
            keyConstraints.forEach(constraint -> nodePatternsWithConstraints.add(
                    new NodePattern(constraint.getLabel(), new HashSet<>(constraint.getProperties()))));
            uniqueConstraints.forEach(constraint -> nodePatternsWithConstraints.add(
                    new NodePattern(constraint.getLabel(), new HashSet<>(constraint.getProperties()))));
        }
    }

    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        checkNode(
                String.format("$.targets.relationships[%d].start_node_reference", index),
                target.getStartNodeReference());
        checkNode(String.format("$.targets.relationships[%d].end_node_reference", index), target.getEndNodeReference());
    }

    @Override
    public boolean report(Builder builder) {
        errorMessages.forEach((path, error) -> {
            builder.addError(path, ERROR_CODE, error);
        });
        return !errorMessages.isEmpty();
    }

    private void checkNode(String path, NodeReference nodeReference) {
        var nodeName = nodeReference.getName();
        if (!targetsWithoutConstraint.contains(nodeName)) {
            return;
        }
        // match call can be costly, only run it for node targets without any key/unique constraints
        var keyMappings = nodeReference.getKeyMappings();
        var matchResult = visitedNodeShapes.get(nodeName).match(nodePatternsWithConstraints, keyMappings);
        if (!matchResult.matches()) {
            var unmatchedList = matchResult.unmatchedProperties().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("','", "'", "'"));
            errorMessages.put(
                    path,
                    String.format(
                            "Node '%s' must define key or unique constraints for %s the properties (%s)",
                            nodeName, keyMappings.isEmpty() ? "any of" : "all of", unmatchedList));
        }
    }

    private static final class NodeShape {
        private final List<String> labels;
        private final List<String> properties;

        public NodeShape(NodeTarget target) {
            this.labels = target.getLabels();
            this.properties = target.getProperties().stream()
                    .map(PropertyMapping::getTargetProperty)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodeShape)) return false;
            NodeShape that = (NodeShape) o;
            return Objects.equals(labels, that.labels) && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(labels, properties);
        }

        @Override
        public String toString() {
            return "NodeTargetShape{" + "labels=" + labels + ", properties=" + properties + '}';
        }

        public PropertyMatchResult match(Set<NodePattern> patterns, List<KeyMapping> keyMappings) {
            var allLabels = patterns.stream().map(NodePattern::getLabel).collect(Collectors.toSet());
            var nodeLabels = new HashSet<>(this.labels);
            var nodeKeys = keyMappings.stream().map(KeyMapping::getNodeProperty).collect(Collectors.toSet());
            var nodeProperties = new HashSet<>(this.properties);
            var nodePropertiesToCheck = nodeKeys.isEmpty() ? nodeProperties : nodeKeys;

            // first short-circuit attempt: if no overlap with other node labels, then let's stop there
            var overlappingLabels = Sets.intersection(nodeLabels, allLabels);
            if (overlappingLabels.isEmpty()) {
                return new PropertyMatchResult(false, nodePropertiesToCheck);
            }

            // second short-circuit attempt: if no overlap with other nodes' key properties, then let's stop there
            var labelMatchingPatterns = overlappingLabels.stream()
                    .flatMap(label -> patterns.stream()
                            .filter(nodePattern -> nodePattern.getLabel().equals(label)))
                    .collect(Collectors.toSet());
            var overlappingLabelProperties = labelMatchingPatterns.stream()
                    .flatMap(n -> n.getProperties().stream())
                    .collect(Collectors.toSet());
            if (Sets.intersection(nodePropertiesToCheck, overlappingLabelProperties)
                    .isEmpty()) {
                return new PropertyMatchResult(false, nodePropertiesToCheck);
            }

            // final attempt: there is an overlap, but it needs to match key/unique constraints
            var coveredProperties = Sets.generateNonEmptySubsets(nodePropertiesToCheck)
                    .flatMap(combination -> this.labels.stream().map(label -> new NodePattern(label, combination)))
                    .filter(labelMatchingPatterns::contains)
                    // all key properties must match a key/unique constraint pattern
                    // (individually or as part of a larger subset)
                    .flatMap(pattern -> pattern.properties.stream())
                    .collect(Collectors.toSet());
            if (nodeKeys.isEmpty()) {
                // no key mapping override, so at least 1 property covered by a key constraint is enough
                var success = !coveredProperties.isEmpty();
                return new PropertyMatchResult(success, success ? Set.of() : nodeProperties);
            }
            // key mapping override means key properties are knows
            // all key properties must be backed by a key/unique constraint
            var uncoveredKeys = Sets.difference(nodeKeys, coveredProperties);
            return new PropertyMatchResult(uncoveredKeys.isEmpty(), uncoveredKeys);
        }
    }

    private static final class PropertyMatchResult {
        private final boolean matches;
        private final Set<String> unmatchedProperties;

        public PropertyMatchResult(boolean matches, Set<String> unmatchedProperties) {
            this.matches = matches;
            this.unmatchedProperties = unmatchedProperties;
        }

        public boolean matches() {
            return matches;
        }

        public List<String> unmatchedProperties() {
            return unmatchedProperties.stream().sorted().collect(Collectors.toList());
        }
    }

    private static final class NodePattern {

        private final String label;
        private final Set<String> properties;

        public NodePattern(String label, Set<String> properties) {
            this.label = label;
            this.properties = properties;
        }

        public String getLabel() {
            return label;
        }

        public Set<String> getProperties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodePattern)) return false;
            NodePattern that = (NodePattern) o;
            return Objects.equals(label, that.label) && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, properties);
        }

        @Override
        public String toString() {
            return "NodePattern{" + "label='" + label + '\'' + ", properties=" + properties + '}';
        }
    }
}
