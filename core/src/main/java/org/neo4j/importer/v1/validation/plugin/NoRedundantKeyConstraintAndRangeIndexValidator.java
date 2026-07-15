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

public class NoRedundantKeyConstraintAndRangeIndexValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "NRDC-003";

    private final Map<String, List<List<String>>> invalidPaths;

    public NoRedundantKeyConstraintAndRangeIndexValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingLabelInRangeIndexValidator.class,
                NoDanglingLabelInKeyConstraintValidator.class,
                NoDanglingPropertyInRangeIndexValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class,
                NoDuplicatedSchemaDefinitionValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        var rangeIndexPaths = new LinkedHashMap<SchemaNodePattern, List<String>>();
        var rangeIndexBasePath = String.format("$.targets.nodes[%d].schema.range_indexes", index);
        var rangeIndexes = schema.getRangeIndexes();
        for (int i = 0; i < rangeIndexes.size(); i++) {
            var rangeIndex = rangeIndexes.get(i);
            var labelAndProps = new SchemaNodePattern(rangeIndex.getLabel(), rangeIndex.getProperties());
            rangeIndexPaths
                    .computeIfAbsent(labelAndProps, (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", rangeIndexBasePath, i));
        }
        var keyPaths = new LinkedHashMap<SchemaNodePattern, List<String>>();
        var keyBasePath = String.format("$.targets.nodes[%d].schema.key_constraints", index);
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            var constraint = keyConstraints.get(i);
            var labelAndProp = new SchemaNodePattern(constraint.getLabel(), constraint.getProperties());
            keyPaths.computeIfAbsent(labelAndProp, (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", keyBasePath, i));
        }
        var redundancies = redundancies(rangeIndexPaths, keyPaths);

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
        var rangeIndexPaths = new LinkedHashMap<List<String>, List<String>>();
        var rangeIndexBasePath = String.format("$.targets.relationships[%d].schema.range_indexes", index);
        var rangeIndexes = schema.getRangeIndexes();
        for (int i = 0; i < rangeIndexes.size(); i++) {
            var rangeIndex = rangeIndexes.get(i);
            rangeIndexPaths
                    .computeIfAbsent(rangeIndex.getProperties(), (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", rangeIndexBasePath, i));
        }
        var keyPaths = new LinkedHashMap<List<String>, List<String>>();
        var keyBasePath = String.format("$.targets.relationships[%d].schema.key_constraints", index);
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            var constraint = keyConstraints.get(i);
            keyPaths.computeIfAbsent(constraint.getProperties(), (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", keyBasePath, i));
        }
        var redundancies = redundancies(rangeIndexPaths, keyPaths);

        if (!redundancies.isEmpty()) {
            var schemaPath = String.format("$.targets.relationships[%d].schema", index);
            invalidPaths.put(schemaPath, redundancies);
        }
    }

    // a redundancy exists only when the same label/properties (or properties, for relationships) is covered by both a
    // range index and a key constraint. Two range indexes (or two key constraints) sharing the same properties are not
    // reported here.
    private static <K> List<List<String>> redundancies(
            Map<K, List<String>> rangeIndexPaths, Map<K, List<String>> keyPaths) {
        var redundancies = new ArrayList<List<String>>();
        rangeIndexPaths.forEach((pattern, rangeIndexDefinitions) -> {
            var keyDefinitions = keyPaths.get(pattern);
            if (keyDefinitions != null) {
                var redundantDefinitions = new ArrayList<String>(rangeIndexDefinitions.size() + keyDefinitions.size());
                redundantDefinitions.addAll(rangeIndexDefinitions);
                redundantDefinitions.addAll(keyDefinitions);
                redundancies.add(redundantDefinitions);
            }
        });
        return redundancies;
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
                            "%s defines redundant key constraint and range index: %s", schemaPath, redundantDefs));
        }));
        return !invalidPaths.isEmpty();
    }
}
