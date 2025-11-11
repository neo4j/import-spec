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
import java.util.function.Supplier;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingSourceValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-001";

    private final Set<String> sourceNames;
    private final Map<String, String> pathToSourceName;

    public NoDanglingSourceValidator() {
        sourceNames = new HashSet<>();
        pathToSourceName = new LinkedHashMap<>();
    }

    @Override
    public void visitSource(int index, Source source) {
        sourceNames.add(source.getName());
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        checkSource(target, () -> String.format("$.targets.nodes[%d]", index));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        checkSource(target, () -> String.format("$.targets.relationships[%d]", index));
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        checkSource(target, () -> String.format("$.targets.queries[%d]", index));
    }

    @Override
    public boolean report(Builder builder) {
        pathToSourceName.forEach((path, source) -> {
            builder.addError(
                    path,
                    ERROR_CODE,
                    String.format(
                            "%s refers to the non-existing source \"%s\". Possible names are: \"%s\"",
                            path, source, String.join("\", \"", sourceNames)));
        });
        return !pathToSourceName.isEmpty();
    }

    private void checkSource(Target target, Supplier<String> path) {
        String source = target.getSource();
        if (!sourceNames.contains(source)) {
            pathToSourceName.put(path.get(), source);
        }
    }
}
