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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoKeylessRelationshipNodeValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "NKRN-001";

    private final Set<String> keylessNodes = new HashSet<>();

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
        var schema = target.getSchema();
        if (schema.getKeyConstraints().isEmpty()
                && schema.getUniqueConstraints().isEmpty()) {
            keylessNodes.add(target.getName());
        }
    }

    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var startNode = target.getStartNodeReference().getName();
        if (keylessNodes.contains(startNode)) {
            errorMessages.put(
                    String.format("$.targets.relationships[%d].start_node_reference", index),
                    String.format(
                            "Node %s must define a key or unique constraint for property id, none found", startNode));
        }
        var endNode = target.getEndNodeReference().getName();
        if (keylessNodes.contains(endNode)) {
            errorMessages.put(
                    String.format("$.targets.relationships[%d].end_node_reference", index),
                    String.format(
                            "Node %s must define a key or unique constraint for property id, none found", endNode));
        }
    }

    @Override
    public boolean report(Builder builder) {
        errorMessages.forEach((path, error) -> {
            builder.addError(path, ERROR_CODE, error);
        });
        return !errorMessages.isEmpty();
    }
}
