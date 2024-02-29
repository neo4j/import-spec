/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingNodeReferenceValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DANG-003";

    private final Set<String> names;
    private final Map<String, String> invalidPathToNodeReferences;

    public NoDanglingNodeReferenceValidator() {
        names = new LinkedHashSet<>();
        invalidPathToNodeReferences = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        names.add(target.getName());
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        String startNodeRef = target.getStartNodeReference();
        if (startNodeRef != null && !names.contains(startNodeRef)) {
            invalidPathToNodeReferences.put(
                    String.format("$.targets.relationships[%d].start_node_reference", index), startNodeRef);
        }
        String endNodeRef = target.getEndNodeReference();
        if (endNodeRef != null && !names.contains(endNodeRef)) {
            invalidPathToNodeReferences.put(
                    String.format("$.targets.relationships[%d].end_node_reference", index), endNodeRef);
        }
    }

    @Override
    public void accept(Builder builder) {
        invalidPathToNodeReferences.forEach((path, invalidNodeReference) -> {
            builder.addError(
                    path,
                    ERROR_CODE,
                    String.format("%s refers to a non-existing node target \"%s\".", path, invalidNodeReference));
        });
    }
}
