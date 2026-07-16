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

import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidator;

// for the same label/type, any subset of the key properties also matched by an existence constraint constitute a
// redundancy.
// while the database allows this, import-spec forbids it
public class NoRedundantKeyAndExistenceConstraintsValidator extends AbstractRedundantSchemaValidator {

    private static final String ERROR_CODE = "NRDC-001";

    public NoRedundantKeyAndExistenceConstraintsValidator() {
        super(ERROR_CODE, "key and existence constraints");
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingLabelInExistenceConstraintValidator.class,
                NoDanglingLabelInKeyConstraintValidator.class,
                NoDanglingPropertyInExistenceConstraintValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class,
                NoDuplicatedSchemaDefinitionValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        var existencePaths = index(
                schema.getExistenceConstraints(),
                String.format("$.targets.nodes[%d].schema.existence_constraints", index),
                (constraint) -> new SchemaNodePattern(constraint.getLabel(), constraint.getProperty()));
        // a key constraint is redundant per individual property it shares with an existence constraint
        var keyPaths = indexMulti(
                schema.getKeyConstraints(),
                String.format("$.targets.nodes[%d].schema.key_constraints", index),
                (constraint) -> constraint.getProperties().stream()
                        .map((property) -> new SchemaNodePattern(constraint.getLabel(), property))
                        .collect(Collectors.toList()));
        recordRedundancies(String.format("$.targets.nodes[%d].schema", index), redundancies(existencePaths, keyPaths));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var schema = target.getSchema();
        if (schema.isEmpty()) {
            return;
        }
        var existencePaths = index(
                schema.getExistenceConstraints(),
                String.format("$.targets.relationships[%d].schema.existence_constraints", index),
                (constraint) -> constraint.getProperty());
        // a key constraint is redundant per individual property it shares with an existence constraint
        var keyPaths = indexMulti(
                schema.getKeyConstraints(),
                String.format("$.targets.relationships[%d].schema.key_constraints", index),
                (constraint) -> constraint.getProperties());
        recordRedundancies(
                String.format("$.targets.relationships[%d].schema", index), redundancies(existencePaths, keyPaths));
    }
}
