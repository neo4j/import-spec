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
import java.util.Locale;
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

public class NoIncompleteNodeReferenceKeyMatchValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "NINR-001";

    private final Map<String, List<LookupProperties>> possibleLookups = new HashMap<>();

    private final Map<String, String> incompleteKeyMappings = new LinkedHashMap<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDanglingKeyInNodeReferenceKeyMappingsValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        nodeTarget.getSchema().getKeyConstraints().forEach(constraint -> possibleLookups
                .computeIfAbsent(nodeTarget.getName(), (name) -> new ArrayList<>())
                .add(LookupProperties.key(constraint)));
        nodeTarget.getSchema().getUniqueConstraints().forEach(constraint -> possibleLookups
                .computeIfAbsent(nodeTarget.getName(), (name) -> new ArrayList<>())
                .add(LookupProperties.unique(constraint)));
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
        incompleteKeyMappings.forEach((path, message) -> {
            builder.addError(path, ERROR_CODE, message);
        });
        return !incompleteKeyMappings.isEmpty();
    }

    private void visitKeyMappings(String path, NodeReference nodeReference) {
        var keyMappings = nodeReference.getKeyMappings();
        if (keyMappings.isEmpty()) {
            return;
        }
        var lookupProperties = possibleLookups.get(nodeReference.getName());
        if (lookupProperties == null) {
            // keyless node, ignoring (another upstream validator takes care of this)
            return;
        }
        var mappedProperties =
                keyMappings.stream().map(KeyMapping::getNodeProperty).collect(Collectors.toSet());
        var matchedProperties = lookupProperties.stream()
                .filter(lookup -> mappedProperties.containsAll(lookup.getProperties()))
                .flatMap(lookup -> lookup.getProperties().stream())
                .collect(Collectors.toSet());
        for (int i = 0; i < keyMappings.size(); i++) {
            var mappedKey = keyMappings.get(i).getNodeProperty();
            if (!matchedProperties.contains(mappedKey)) {
                var maybeClosestMatch = lookupProperties.stream()
                        .filter(lookup -> lookup.includes(mappedKey))
                        .max((lookup1, lookup2) -> sortLookups(lookup1, lookup2, mappedProperties));
                var mappingPath = String.format("%s[%d]", path, i);
                if (maybeClosestMatch.isEmpty()) {
                    // prop is invalid or not a key/not unique, ignoring (another upstream validator takes care of this)
                    return;
                }
                var closestMatch = maybeClosestMatch.get();
                var missingKeys = closestMatch.getProperties().stream()
                        .filter(property -> !mappedProperties.contains(property))
                        .collect(Collectors.toList());
                var error = String.format(
                        "Insufficient key mapping for node reference '%s'. Please also map ['%s'] alongside '%s' to fully match the node target's %s constraint '%s'",
                        nodeReference.getName(),
                        String.join("', '", missingKeys),
                        mappedKey,
                        closestMatch.getType().toString().toLowerCase(Locale.ROOT),
                        closestMatch.getConstraintName());
                incompleteKeyMappings.put(mappingPath, error);
            }
        }
    }

    private static int sortLookups(LookupProperties lookup1, LookupProperties lookup2, Set<String> mappedProperties) {
        var count1 = lookup1.countMatches(mappedProperties);
        var count2 = lookup2.countMatches(mappedProperties);
        if (count1 == count2) {
            // more specific key/unique definitions win (i.e., key/unique defs with more properties)
            // this is because such definitions are likely to offer a lower cardinality and a faster lookup
            // when leveraged in queries
            return lookup1.getPropertyCount() - lookup2.getPropertyCount();
        }
        return count1 - count2;
    }

    private static class LookupProperties {

        private final LookupType type;
        private final String constraintName;
        private final List<String> properties;

        private LookupProperties(LookupType type, String constraintName, List<String> properties) {
            this.type = type;
            this.constraintName = constraintName;
            this.properties = properties;
        }

        public static LookupProperties key(NodeKeyConstraint constraint) {
            return new LookupProperties(LookupType.KEY, constraint.getName(), constraint.getProperties());
        }

        public static LookupProperties unique(NodeUniqueConstraint constraint) {
            return new LookupProperties(LookupType.UNIQUE, constraint.getName(), constraint.getProperties());
        }

        public LookupType getType() {
            return type;
        }

        public List<String> getProperties() {
            return properties;
        }

        public String getConstraintName() {
            return constraintName;
        }

        public boolean includes(String property) {
            return properties.contains(property);
        }

        public int countMatches(Set<String> mappedProperties) {
            return (int) properties.stream().filter(mappedProperties::contains).count();
        }

        public int getPropertyCount() {
            return properties.size();
        }
    }

    enum LookupType {
        UNIQUE,
        KEY
    }
}
