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
import java.util.Map;
import java.util.Set;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingKeyInNodeReferenceKeyMappingsValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-023";

    private final Map<String, Set<String>> nodeKeyOrUniqueProperties = new HashMap<>();

    private final Map<String, String> invalidKeyMappings = new LinkedHashMap<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingActiveNodeReferenceValidator.class,
                NoDanglingPropertyInNodeReferenceKeyMappingsValidator.class,
                NoKeylessRelationshipNodeValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var keyOrUniqueProps = new HashSet<String>(target.getProperties().size());
        target.getSchema()
                .getKeyConstraints()
                .forEach(constraint -> keyOrUniqueProps.addAll(constraint.getProperties()));
        target.getSchema()
                .getUniqueConstraints()
                .forEach(constraint -> keyOrUniqueProps.addAll(constraint.getProperties()));
        nodeKeyOrUniqueProperties.put(target.getName(), keyOrUniqueProps);
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var startNodeReference = target.getStartNodeReference();
        Set<String> startKeyOrUniqueProps =
                nodeKeyOrUniqueProperties.getOrDefault(startNodeReference.getName(), Set.of());
        if (!startKeyOrUniqueProps.isEmpty()) {
            var startKeyMappings = startNodeReference.getKeyMappings();
            for (int i = 0; i < startKeyMappings.size(); i++) {
                var mapping = startKeyMappings.get(i);
                var property = mapping.getNodeProperty();
                if (!startKeyOrUniqueProps.contains(property)) {
                    var path = String.format(
                            "$.relationships[%d].start_node_reference.key_mappings[%d].node_property", index, i);
                    var error = String.format(
                            "Property '%s' is not part of start node target's %s key and unique properties",
                            property, startNodeReference.getName());
                    invalidKeyMappings.put(path, error);
                }
            }
        }
        var endNodeReference = target.getEndNodeReference();
        Set<String> endKeyOrUniqueProps = nodeKeyOrUniqueProperties.getOrDefault(endNodeReference.getName(), Set.of());
        if (!endKeyOrUniqueProps.isEmpty()) {
            var endKeyMappings = endNodeReference.getKeyMappings();
            for (int i = 0; i < endKeyMappings.size(); i++) {
                var mapping = endKeyMappings.get(i);
                var property = mapping.getNodeProperty();
                if (!endKeyOrUniqueProps.contains(property)) {
                    var path = String.format(
                            "$.relationships[%d].end_node_reference.key_mappings[%d].node_property", index, i);
                    var error = String.format(
                            "Property '%s' is not part of end node target's %s key and unique properties",
                            property, endNodeReference.getName());
                    invalidKeyMappings.put(path, error);
                }
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidKeyMappings.forEach((path, message) -> {
            builder.addError(path, ERROR_CODE, message);
        });
        return !invalidKeyMappings.isEmpty();
    }
}
