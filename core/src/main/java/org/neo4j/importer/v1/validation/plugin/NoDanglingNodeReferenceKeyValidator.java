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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingNodeReferenceKeyValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DANG-024";

    private final Map<String, List<NodeKeyConstraint>> nodeKeysPerTarget;
    private final Map<String, String> invalidKeyMappings;

    public NoDanglingNodeReferenceKeyValidator() {
        this.nodeKeysPerTarget = new HashMap<>();
        this.invalidKeyMappings = new HashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingActiveNodeReferenceValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class,
                NoDanglingNodeReferencePropertyValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        nodeKeysPerTarget.put(nodeTarget.getName(), nodeTarget.getSchema().getKeyConstraints());
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
        return invalidKeyMappings.isEmpty();
    }

    private void visitKeyMappings(String path, NodeReference nodeReference) {
        var keyMappings = nodeReference.getKeyMappings();
        if (keyMappings.isEmpty()) {
            return;
        }

        var nodeKeyConstraints = nodeKeysPerTarget.get(nodeReference.getName());
        if (nodeKeyConstraints == null) {
            invalidKeyMappings.put(path, nodeReference.getName() + " must define key constraints but none were found");
            return;
        }
        var keyProperties = nodeKeyConstraints.stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .collect(Collectors.toSet());

        for (int i = 0; i < keyMappings.size(); i++) {
            var mappedProperty = keyMappings.get(i).getTargetProperty();
            if (!keyProperties.contains(mappedProperty)) {
                var mappingPath = String.format("%s.key_mappings[%d].target_key_property", path, i);
                var error = String.format(
                        "Node %s must define a key constraint for property %s, none found",
                        nodeReference.getName(), mappedProperty);
                invalidKeyMappings.put(mappingPath, error);
            }
        }
    }
}
