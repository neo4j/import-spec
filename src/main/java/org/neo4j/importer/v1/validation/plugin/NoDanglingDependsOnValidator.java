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
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingDependsOnValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-002";

    private final Set<String> names;
    private final Map<String, String> pathToDependsOn;

    public NoDanglingDependsOnValidator() {
        names = new LinkedHashSet<>();
        pathToDependsOn = new LinkedHashMap<>();
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
    public void visitAction(int index, Action action) {
        track(action, String.format("$.actions[%d]", index));
    }

    @Override
    public void accept(Builder builder) {
        pathToDependsOn.entrySet().stream()
                .filter(entry -> !names.contains(entry.getValue()))
                .forEachOrdered(entry -> {
                    String path = entry.getKey();
                    String invalidDependsOn = entry.getValue();
                    builder.addError(
                            path,
                            ERROR_CODE,
                            String.format(
                                    "%s depends on a non-existing action or target \"%s\".", path, invalidDependsOn));
                });
    }

    private void track(Target target, String path) {
        names.add(target.getName());
        String dependsOn = target.getDependsOn();
        if (dependsOn != null) {
            pathToDependsOn.put(path, dependsOn);
        }
    }

    private void track(Action action, String path) {
        names.add(action.getName());
        String dependsOn = action.getDependsOn();
        if (dependsOn != null) {
            pathToDependsOn.put(path, dependsOn);
        }
    }
}
