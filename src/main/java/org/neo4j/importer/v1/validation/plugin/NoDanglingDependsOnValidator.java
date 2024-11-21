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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.neo4j.importer.v1.graph.Pair;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingDependsOnValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-002";

    private final Set<String> names;
    private final Map<String, List<String>> pathToDependencies;

    public NoDanglingDependsOnValidator() {
        names = new LinkedHashSet<>();
        pathToDependencies = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        track(target, String.format("$.targets.nodes[%d]", index));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        track(target, String.format("$.targets.relationships[%d]", index));
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        track(target, String.format("$.targets.queries[%d]", index));
    }

    @Override
    public boolean report(Builder builder) {
        AtomicBoolean result = new AtomicBoolean(false);
        pathToDependencies.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(dependency -> Pair.of(entry.getKey(), dependency)))
                .filter(pair -> !names.contains(pair.getSecond()))
                .forEachOrdered(pair -> {
                    result.set(true);
                    String path = pair.getFirst();
                    String invalidDependsOn = pair.getSecond();
                    builder.addError(
                            path,
                            ERROR_CODE,
                            String.format("%s depends on a non-existing target \"%s\".", path, invalidDependsOn));
                });
        return result.get();
    }

    private void track(Target target, String path) {
        names.add(target.getName());
        pathToDependencies.put(path, target.getDependencies());
    }
}
