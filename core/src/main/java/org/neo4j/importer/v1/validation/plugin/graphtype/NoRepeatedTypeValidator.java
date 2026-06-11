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
package org.neo4j.importer.v1.validation.plugin.graphtype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.Configuration;
import org.neo4j.importer.v1.config.Features;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoRepeatedTypeValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "GTRT-001";

    private final Map<String, List<String>> typePaths = new LinkedHashMap<>();

    private final AtomicBoolean skipValidation = new AtomicBoolean(true);

    @Override
    public void visitConfiguration(Configuration configuration) {
        skipValidation.set(!Features.isGraphTypeEnabled(configuration));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        if (skipValidation.get()) {
            return;
        }
        var path = String.format("$.targets.relationships[%d].type", index);
        typePaths.computeIfAbsent(target.getType(), (e) -> new ArrayList<>()).add(path);
    }

    @Override
    public boolean report(Builder builder) {
        var repeatedTypes = typePaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toList());
        repeatedTypes.forEach(entry -> {
            var repeatedType = entry.getKey();
            var paths = entry.getValue();
            builder.addError(
                    paths.get(0),
                    ERROR_CODE,
                    String.format(
                            "Graph type cannot be generated: the type %s is defined more than once (offending occurrences in %s)",
                            repeatedType, String.join(", ", paths.subList(1, paths.size()))));
        });
        return !repeatedTypes.isEmpty();
    }
}
