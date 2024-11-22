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

import static org.neo4j.importer.v1.validation.plugin.Duplicate.findDuplicates;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedDependencyValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DUPL-003";

    private final Map<String, Duplicate<String>> pathToDuplicates = new LinkedHashMap<>();

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        String path = String.format("$.targets.nodes[%d].depends_on", index);
        track(path, target.getDependencies());
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        String path = String.format("$.targets.relationships[%d].depends_on", index);
        track(path, target.getDependencies());
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        String path = String.format("$.targets.queries[%d].depends_on", index);
        track(path, target.getDependencies());
    }

    @Override
    public boolean report(Builder builder) {
        if (pathToDuplicates.isEmpty()) {
            return false;
        }
        pathToDuplicates.forEach((sourcePath, duplicate) -> {
            builder.addError(
                    sourcePath,
                    ERROR_CODE,
                    String.format(
                            "%s defines dependency \"%s\" %d times, it must be defined at most once",
                            sourcePath, duplicate.getValue(), duplicate.getCount()));
        });
        return true;
    }

    private void track(String path, List<String> dependencies) {
        findDuplicates(dependencies).forEach(duplicate -> pathToDuplicates.put(path, duplicate));
    }
}
