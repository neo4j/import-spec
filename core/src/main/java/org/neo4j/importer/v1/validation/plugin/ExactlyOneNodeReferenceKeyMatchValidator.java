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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeUniqueConstraint;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class ExactlyOneNodeReferenceKeyMatchValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "EONR-001";

    private final Map<String, List<LookupConstraint>> possibleLookups = new HashMap<>();

    private final Map<String, String> invalidKeyMappings = new LinkedHashMap<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingKeyInNodeReferenceKeyMappingsValidator.class,
                NoIncompleteNodeReferenceKeyMatchValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        var schema = nodeTarget.getSchema();
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            possibleLookups
                    .computeIfAbsent(nodeTarget.getName(), (name) -> new ArrayList<>())
                    .add(LookupConstraint.key(
                            keyConstraints.get(i),
                            String.format("$.targets.nodes[%d].schema.key_constraints[%d]", index, i)));
        }
        var uniqueConstraints = schema.getUniqueConstraints();
        for (int i = 0; i < uniqueConstraints.size(); i++) {
            possibleLookups
                    .computeIfAbsent(nodeTarget.getName(), (name) -> new ArrayList<>())
                    .add(LookupConstraint.unique(
                            uniqueConstraints.get(i),
                            String.format("$.targets.nodes[%d].schema.unique_constraints[%d]", index, i)));
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget relationshipTarget) {
        visitKeyMappings(
                String.format("$.targets.relationships[%d].start_node_reference.key_mappings", index),
                relationshipTarget.getStartNodeReference());
        visitKeyMappings(
                String.format("$.targets.relationships[%d].end_node_reference.key_mappings", index),
                relationshipTarget.getEndNodeReference());
    }

    @Override
    public boolean report(Builder builder) {
        invalidKeyMappings.forEach((path, message) -> {
            builder.addError(path, ERROR_CODE, message);
        });
        return !invalidKeyMappings.isEmpty();
    }

    private void visitKeyMappings(String path, NodeReference nodeReference) {
        var keyMappings = nodeReference.getKeyMappings();
        if (keyMappings.isEmpty()) {
            return;
        }
        var lookupConstraints = possibleLookups.get(nodeReference.getName());
        if (lookupConstraints == null) {
            return;
        }

        var mappedProperties =
                keyMappings.stream().map(KeyMapping::getNodeProperty).collect(Collectors.toSet());
        var matchedConstraints = lookupConstraints.stream()
                .filter(lookup -> mappedProperties.equals(lookup.getProperties()))
                .collect(Collectors.toList());
        if (matchedConstraints.size() == 1) {
            return;
        }

        var error = String.format(
                "Invalid key mapping for node reference '%s'. Expected it to exactly match 1 node target key or unique constraint, but it exactly matches %s%s",
                nodeReference.getName(),
                formatConstraintCount(matchedConstraints.size()),
                describeMatchedConstraintPaths(matchedConstraints));
        invalidKeyMappings.put(path, error);
    }

    private static String formatConstraintCount(int count) {
        var constraint = count == 0 ? "constraint" : "constraints";
        return String.format("%d node target key or unique %s", count, constraint);
    }

    private static String describeMatchedConstraintPaths(List<LookupConstraint> matchedConstraints) {
        if (matchedConstraints.isEmpty()) {
            return ".";
        }
        var matchedConstraintPaths =
                matchedConstraints.stream().map(LookupConstraint::getPath).collect(Collectors.joining("\n\t"));
        return String.format(":\n\t%s", matchedConstraintPaths);
    }

    private static final class LookupConstraint {

        private final Set<String> properties;
        private final String path;

        private LookupConstraint(List<String> properties, String path) {
            this.properties = Set.copyOf(properties);
            this.path = path;
        }

        static LookupConstraint key(NodeKeyConstraint constraint, String path) {
            return new LookupConstraint(constraint.getProperties(), path);
        }

        static LookupConstraint unique(NodeUniqueConstraint constraint, String path) {
            return new LookupConstraint(constraint.getProperties(), path);
        }

        Set<String> getProperties() {
            return properties;
        }

        String getPath() {
            return path;
        }
    }
}
