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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingPropertyInNodeReferenceKeyMappingsValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DANG-023";

    private final Map<String, Set<String>> nodeTargets;
    private final Map<String, String> invalidPathToKeyMappings;

    public NoDanglingPropertyInNodeReferenceKeyMappingsValidator() {
        this.nodeTargets = new LinkedHashMap<>();
        this.invalidPathToKeyMappings = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDanglingActiveNodeReferenceValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        nodeTargets.put(
                nodeTarget.getName(),
                nodeTarget.getProperties().stream()
                        .map(PropertyMapping::getTargetProperty)
                        .collect(Collectors.toSet()));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget relationshipTarget) {
        validateNodeReference(
                index,
                relationshipTarget.getStartNodeReference(),
                "$.targets.relationships[%d].start_node_reference.key_mappings[%d].node_property");
        validateNodeReference(
                index,
                relationshipTarget.getEndNodeReference(),
                "$.targets.relationships[%d].end_node_reference.key_mappings[%d].node_property");
    }

    private void validateNodeReference(int relationshipIndex, NodeReference nodeReference, String path) {
        var nodeKeyMappings = nodeReference.getKeyMappings();
        if (nodeKeyMappings.isEmpty()) {
            return;
        }
        var targetProperties = nodeTargets.get(nodeReference.getName());
        for (int mappingIndex = 0; mappingIndex < nodeKeyMappings.size(); mappingIndex++) {
            var keyMapping = nodeKeyMappings.get(mappingIndex);
            if (!targetProperties.contains(keyMapping.getNodeProperty())) {
                invalidPathToKeyMappings.put(
                        String.format(path, relationshipIndex, mappingIndex), keyMapping.getNodeProperty());
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPathToKeyMappings.forEach((path, invalidNodeReference) -> {
            builder.addError(
                    path,
                    ERROR_CODE,
                    String.format("%s refers to a non-existing node property \"%s\".", path, invalidNodeReference));
        });
        return !invalidPathToKeyMappings.isEmpty();
    }
}
