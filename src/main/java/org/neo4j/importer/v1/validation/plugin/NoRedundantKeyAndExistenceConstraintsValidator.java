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
        if (schema == null) {
            return;
        }
        var paths = new LinkedHashMap<ConstraintPattern, List<String>>();
        var existenceBasePath = String.format("$.targets.nodes[%d].schema.existence_constraints", index);
        var existenceConstraints = schema.getExistenceConstraints();
        for (int i = 0; i < existenceConstraints.size(); i++) {
            var constraint = existenceConstraints.get(i);
            var labelAndProp = new ConstraintPattern(constraint.getLabel(), constraint.getProperty());
            paths.computeIfAbsent(labelAndProp, (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", existenceBasePath, i));
        }
        var keyBasePath = String.format("$.targets.nodes[%d].schema.key_constraints", index);
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            var constraint = keyConstraints.get(i);
            for (String property : constraint.getProperties()) {
                var labelAndProp = new ConstraintPattern(constraint.getLabel(), property);
                paths.computeIfAbsent(labelAndProp, (key) -> new ArrayList<>(1))
                        .add(String.format("%s[%d]", keyBasePath, i));
            }
        }
        List<List<String>> redundancies =
                paths.values().stream().filter(allPaths -> allPaths.size() > 1).collect(Collectors.toList());

        var schemaPath = String.format("$.targets.nodes[%d].schema", index);
        invalidPaths.put(schemaPath, redundancies);
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var schema = target.getSchema();
        if (schema == null) {
            return;
        }
        var paths = new LinkedHashMap<String, List<String>>();
        var existenceBasePath = String.format("$.targets.relationships[%d].schema.existence_constraints", index);
        var existenceConstraints = schema.getExistenceConstraints();
        for (int i = 0; i < existenceConstraints.size(); i++) {
            var constraint = existenceConstraints.get(i);
            paths.computeIfAbsent(constraint.getProperty(), (key) -> new ArrayList<>(1))
                    .add(String.format("%s[%d]", existenceBasePath, i));
        }
        var keyBasePath = String.format("$.targets.relationships[%d].schema.key_constraints", index);
        var keyConstraints = schema.getKeyConstraints();
        for (int i = 0; i < keyConstraints.size(); i++) {
            var constraint = keyConstraints.get(i);
            for (String property : constraint.getProperties()) {
                paths.computeIfAbsent(property, (key) -> new ArrayList<>(1))
                        .add(String.format("%s[%d]", keyBasePath, i));
            }
        }
        List<List<String>> redundancies =
                paths.values().stream().filter(strings -> strings.size() > 1).collect(Collectors.toList());

        var schemaPath = String.format("$.targets.relationships[%d].schema", index);
        invalidPaths.put(schemaPath, redundancies);
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
                            "%s defines overlapping key and existence constraint definitions: %s",
                            schemaPath, redundantDefs));
        }));
        return !invalidPaths.isEmpty();
    }
}
