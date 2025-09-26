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
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoRedundantUniqueConstraintAndRangeIndexValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "NRDC-004";

    private final Map<String, List<List<String>>> invalidPaths;

    public NoRedundantUniqueConstraintAndRangeIndexValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingLabelInRangeIndexValidator.class,
                NoDanglingLabelInUniqueConstraintValidator.class,
                NoDanglingPropertyInRangeIndexValidator.class,
                NoDanglingPropertyInUniqueConstraintValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        var paths = new LinkedHashMap<SchemaNodePattern, List<String>>();
        var rangeIndexBasePath = String.format("$.targets.nodes[%d].schema.range_indexes", index);
        var rangeIndexes = schema.getRangeIndexes();
        for (int i = 0; i < rangeIndexes.size(); i++) {
            var rangeIndex = rangeIndexes.get(i);
            var labelAndProps = new SchemaNodePattern(rangeIndex.getLabel(), rangeIndex.getProperties());
            paths.computeIfAbsent(labelAndProps, (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", rangeIndexBasePath, i));
        }
        var uniqueBasePath = String.format("$.targets.nodes[%d].schema.unique_constraints", index);
        var uniqueConstraints = schema.getUniqueConstraints();
        for (int i = 0; i < uniqueConstraints.size(); i++) {
            var constraint = uniqueConstraints.get(i);
            var labelAndProp = new SchemaNodePattern(constraint.getLabel(), constraint.getProperties());
            paths.computeIfAbsent(labelAndProp, (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", uniqueBasePath, i));
        }
        List<List<String>> redundancies =
                paths.values().stream().filter(allPaths -> allPaths.size() > 1).collect(Collectors.toList());

        if (!redundancies.isEmpty()) {
            var schemaPath = String.format("$.targets.nodes[%d].schema", index);
            invalidPaths.put(schemaPath, redundancies);
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var schema = target.getSchema();
        if (schema.isEmpty()) {
            return;
        }
        var paths = new LinkedHashMap<List<String>, List<String>>();
        var rangeIndexBasePath = String.format("$.targets.relationships[%d].schema.range_indexes", index);
        var rangeIndexes = schema.getRangeIndexes();
        for (int i = 0; i < rangeIndexes.size(); i++) {
            var rangeIndex = rangeIndexes.get(i);
            paths.computeIfAbsent(rangeIndex.getProperties(), (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", rangeIndexBasePath, i));
        }
        var uniqueBasePath = String.format("$.targets.relationships[%d].schema.unique_constraints", index);
        var uniqueConstraints = schema.getUniqueConstraints();
        for (int i = 0; i < uniqueConstraints.size(); i++) {
            var constraint = uniqueConstraints.get(i);
            paths.computeIfAbsent(constraint.getProperties(), (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", uniqueBasePath, i));
        }
        List<List<String>> redundancies =
                paths.values().stream().filter(allPaths -> allPaths.size() > 1).collect(Collectors.toList());

        if (!redundancies.isEmpty()) {
            var schemaPath = String.format("$.targets.relationships[%d].schema", index);
            invalidPaths.put(schemaPath, redundancies);
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((schemaPath, redundancies) -> redundancies.forEach((redundantDefinitions) -> {
            String redundantDefs = redundantDefinitions.stream()
                    .map(def -> def.replace(schemaPath + ".", ""))
                    .collect(Collectors.joining(", "));
            builder.addError(
                    schemaPath,
                    ERROR_CODE,
                    String.format(
                            "%s defines redundant unique constraint and range index: %s", schemaPath, redundantDefs));
        }));
        return !invalidPaths.isEmpty();
    }
}
