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
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingActiveNodeReferenceValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DANG-004";

    private final Set<String> names;
    private final Map<String, String> invalidPathToNodeReferences;

    public NoDanglingActiveNodeReferenceValidator() {
        names = new LinkedHashSet<>();
        invalidPathToNodeReferences = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDanglingNodeReferenceValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        if (!target.isActive()) {
            return;
        }
        names.add(target.getName());
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        if (!target.isActive()) { // an inactive relationship can refer to inactive node targets
            return;
        }
        NodeReference startNodeRef = target.getStartNodeReference();
        if (!names.contains(startNodeRef.getName())) {
            var path = String.format("$.targets.relationships[%d].start_node_reference", index);
            if (!startNodeRef.getKeyMappings().isEmpty()) {
                path = String.format("%s.name", path);
            }
            invalidPathToNodeReferences.put(path, startNodeRef.getName());
        }
        NodeReference endNodeRef = target.getEndNodeReference();
        if (!names.contains(endNodeRef.getName())) {
            var path = String.format("$.targets.relationships[%d].end_node_reference", index);
            if (!endNodeRef.getKeyMappings().isEmpty()) {
                path = String.format("%s.name", path);
            }
            invalidPathToNodeReferences.put(path, endNodeRef.getName());
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPathToNodeReferences.forEach((path, invalidNodeReference) -> {
            builder.addError(
                    path,
                    ERROR_CODE,
                    String.format(
                            "%s belongs to an active target but refers to an inactive node target \"%s\".",
                            path, invalidNodeReference));
        });
        return !invalidPathToNodeReferences.isEmpty();
    }
}
