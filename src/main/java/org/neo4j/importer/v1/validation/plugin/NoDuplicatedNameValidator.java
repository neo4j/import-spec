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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedNameValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DUPL-001";

    private final NameCounter nameCounter;

    public NoDuplicatedNameValidator() {
        nameCounter = new NameCounter(ERROR_CODE);
    }

    @Override
    public void visitSource(int index, Source source) {
        nameCounter.track(source.getName(), String.format("$.sources[%d]", index));
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        nameCounter.track(target.getName(), String.format("$.targets.nodes[%d]", index));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        nameCounter.track(target.getName(), String.format("$.targets.relationships[%d]", index));
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        nameCounter.track(target.getName(), String.format("$.targets.queries[%d]", index));
    }

    @Override
    public void visitAction(int index, Action action) {
        nameCounter.track(action.getName(), String.format("$.actions[%d]", index));
    }

    @Override
    public boolean report(Builder builder) {
        return nameCounter.reportErrorsIfAny(builder);
    }
}

class NameCounter {

    private final Map<String, List<String>> pathsUsingName = new LinkedHashMap<>();
    private final String errorCode;

    public NameCounter(String errorCode) {
        this.errorCode = errorCode;
    }

    public void track(String name, String path) {
        pathsUsingName.computeIfAbsent(name, (ignored) -> new ArrayList<>()).add(String.format("%s.name", path));
    }

    public boolean reportErrorsIfAny(Builder builder) {
        AtomicBoolean result = new AtomicBoolean(false);
        pathsUsingName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> {
                    List<String> paths = entry.getValue();
                    result.set(true);
                    builder.addError(
                            paths.get(0),
                            errorCode,
                            String.format(
                                    "Name \"%s\" is duplicated across the following paths: %s",
                                    entry.getKey(), String.join(", ", paths)));
                });
        return result.get();
    }
}
