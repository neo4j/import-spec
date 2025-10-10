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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoReusedNodeLookupFieldValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "RNLF-001";

    private final Map<String, String> invalidPaths;

    public NoReusedNodeLookupFieldValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDanglingNodeReferenceValidator.class);
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var startNode = target.getStartNodeReference();
        var endNode = target.getEndNodeReference();

        if (startNode.getName().equals(endNode.getName())) {
            // short-circuit for self-linking rels
            return;
        }
        var startNodeSourceKeyFields = sourceFieldsOf(startNode);
        var endNodeSourceKeyFields = sourceFieldsOf(endNode);
        if (!startNodeSourceKeyFields.isEmpty() && startNodeSourceKeyFields.equals(endNodeSourceKeyFields)) {
            var path = String.format("$.targets.relationships[%d]", index);
            var errorMessage = String.format(
                    "the same source fields (%s) cannot be used to map different start and end nodes",
                    String.join(", ", startNodeSourceKeyFields));
            invalidPaths.put(path, errorMessage);
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((path, msg) -> builder.addError(path, ERROR_CODE, msg));
        return !invalidPaths.isEmpty();
    }

    private static Set<String> sourceFieldsOf(NodeReference ref) {
        return ref.getKeyMappings().stream()
                .map(KeyMapping::getSourceField)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
