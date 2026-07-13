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

// for the same label/type, any subset of the key properties also matched by an existence constraint constitute a
// redundancy.
// while the database allows this, import-spec forbids it
public class NoRedundantKeyAndExistenceConstraintsValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "NRDC-001";

    private final Map<String, List<List<String>>> invalidPaths;

    public NoRedundantKeyAndExistenceConstraintsValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingLabelInExistenceConstraintValidator.class,
                NoDanglingLabelInKeyConstraintValidator.class,
                NoDanglingPropertyInExistenceConstraintValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        var existencePaths = new LinkedHashMap<SchemaNodePattern, List<String>>();
        var existenceBasePath = String.format("$.targets.nodes[%d].schema.existence_constraints", index);
        var existenceConstraints = schema.getExistenceConstraints();
        for (int i = 0; i < existenceConstraints.size(); i++) {
            var constraint = existenceConstraints.get(i);
            var labelAndProp = new SchemaNodePattern(constraint.getLabel(), constraint.getProperty());
            existencePaths
                    .computeIfAbsent(labelAndProp, (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", existenceBasePath, i));
        }
        var keyPaths = new LinkedHashMap<SchemaNodePattern, List<String>>();
        var keyBasePath = String.format("$.targets.nodes[%d].schema.key_constraints", index);
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            var constraint = keyConstraints.get(i);
            for (String property : constraint.getProperties()) {
                var labelAndProp = new SchemaNodePattern(constraint.getLabel(), property);
                keyPaths.computeIfAbsent(labelAndProp, (key) -> new ArrayList<>(1))
                        .add(String.format("%s[%d]", keyBasePath, i));
            }
        }
        var redundancies = redundancies(existencePaths, keyPaths);

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
        var existencePaths = new LinkedHashMap<String, List<String>>();
        var existenceBasePath = String.format("$.targets.relationships[%d].schema.existence_constraints", index);
        var existenceConstraints = schema.getExistenceConstraints();
        for (int i = 0; i < existenceConstraints.size(); i++) {
            var constraint = existenceConstraints.get(i);
            existencePaths
                    .computeIfAbsent(constraint.getProperty(), (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", existenceBasePath, i));
        }
        var keyPaths = new LinkedHashMap<String, List<String>>();
        var keyBasePath = String.format("$.targets.relationships[%d].schema.key_constraints", index);
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            var constraint = keyConstraints.get(i);
            for (String property : constraint.getProperties()) {
                keyPaths.computeIfAbsent(property, (key) -> new ArrayList<>(1))
                        .add(String.format("%s[%d]", keyBasePath, i));
            }
        }
        var redundancies = redundancies(existencePaths, keyPaths);

        if (!redundancies.isEmpty()) {
            var schemaPath = String.format("$.targets.relationships[%d].schema", index);
            invalidPaths.put(schemaPath, redundancies);
        }
    }

    // a redundancy exists only when the same label/property (or property, for relationships) is covered by both an
    // existence constraint and a key constraint. Two key constraints sharing a property are not redundant.
    private static <K> List<List<String>> redundancies(
            Map<K, List<String>> existencePaths, Map<K, List<String>> keyPaths) {
        var redundancies = new ArrayList<List<String>>();
        existencePaths.forEach((pattern, existenceDefinitions) -> {
            var keyDefinitions = keyPaths.get(pattern);
            if (keyDefinitions != null) {
                var redundantDefinitions = new ArrayList<String>(existenceDefinitions.size() + keyDefinitions.size());
                redundantDefinitions.addAll(existenceDefinitions);
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
                    String.format("%s defines redundant key and existence constraints: %s", schemaPath, redundantDefs));
        }));
        return !invalidPaths.isEmpty();
    }
}
