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

import java.util.*;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.*;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedSchemaNameValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DUPL-011";
    private final Map<String, List<String>> pathsPerName = new LinkedHashMap<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoRedundantUniqueConstraintAndRangeIndexValidator.class,
                NoRedundantKeyAndUniqueConstraintsValidator.class,
                NoRedundantKeyConstraintAndRangeIndexValidator.class,
                NoRedundantKeyAndExistenceConstraintsValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        String basePath = String.format("$.targets.nodes[%d].schema", index);
        addConstraints(basePath, target.getSchema());
        addIndexes(basePath, target.getSchema());
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        String basePath = String.format("$.targets.relationships[%d].schema", index);
        addConstraints(basePath, target.getSchema());
        addIndexes(basePath, target.getSchema());
    }

    @Override
    public boolean report(Builder builder) {
        var duplicates = pathsPerName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toList());

        duplicates.forEach(dup -> builder.addError(
                dup.getValue().get(0),
                ERROR_CODE,
                String.format(
                        "Constraint or index name \"%s\" must be defined at most once but %d occurrences were found.",
                        dup.getKey(), dup.getValue().size())));
        return !duplicates.isEmpty();
    }

    private void addConstraints(String basePath, Schema schema) {
        addConstraintPaths(basePath, "key_constraints", schema.getKeyConstraints());
        addConstraintPaths(basePath, "unique_constraints", schema.getUniqueConstraints());
        addConstraintPaths(basePath, "existence_constraints", schema.getExistenceConstraints());
        addConstraintPaths(basePath, "type_constraints", schema.getTypeConstraints());
    }

    private void addIndexes(String basePath, Schema schema) {
        addIndexPaths(basePath, "range_indexes", schema.getRangeIndexes());
        addIndexPaths(basePath, "text_indexes", schema.getTextIndexes());
        addIndexPaths(basePath, "point_indexes", schema.getPointIndexes());
        addIndexPaths(basePath, "fulltext_indexes", schema.getFullTextIndexes());
        addIndexPaths(basePath, "vector_indexes", schema.getVectorIndexes());
    }

    private void addConstraintPaths(String basePath, String type, List<? extends Constraint> constraints) {
        for (int i = 0; i < constraints.size(); i++) {
            var item = constraints.get(i);
            String path = String.format("%s.%s[%d]", basePath, type, i);
            pathsPerName
                    .computeIfAbsent(item.getName(), (key) -> new ArrayList<>())
                    .add(path);
        }
    }

    private void addIndexPaths(String basePath, String type, List<? extends Index> constraints) {
        for (int i = 0; i < constraints.size(); i++) {
            var item = constraints.get(i);
            String path = String.format("%s.%s[%d]", basePath, type, i);
            pathsPerName
                    .computeIfAbsent(item.getName(), (key) -> new ArrayList<>())
                    .add(path);
        }
    }
}
