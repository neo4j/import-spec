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
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidator;

// for the same label/type, key properties matched **exactly** with a unique constraint constitute a redundancy.
// note: the following do **not** constitute a redundancy and are therefore allowed (since they can influence the query
// planner):
// - a subset of the key properties are also defined as unique (and vice versa)
// - key properties are also defined as unique but in different order
public class NoRedundantKeyAndUniqueConstraintsValidator extends AbstractRedundantSchemaValidator {

    private static final String ERROR_CODE = "NRDC-002";

    public NoRedundantKeyAndUniqueConstraintsValidator() {
        super(ERROR_CODE, "key and unique constraints");
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingLabelInUniqueConstraintValidator.class,
                NoDanglingLabelInKeyConstraintValidator.class,
                NoDanglingPropertyInUniqueConstraintValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class,
                NoDuplicatedSchemaDefinitionValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        var uniquePaths = index(
                schema.getUniqueConstraints(),
                String.format("$.targets.nodes[%d].schema.unique_constraints", index),
                (constraint) -> new SchemaNodePattern(constraint.getLabel(), constraint.getProperties()));
        var keyPaths = index(
                schema.getKeyConstraints(),
                String.format("$.targets.nodes[%d].schema.key_constraints", index),
                (constraint) -> new SchemaNodePattern(constraint.getLabel(), constraint.getProperties()));
        recordRedundancies(String.format("$.targets.nodes[%d].schema", index), redundancies(uniquePaths, keyPaths));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var schema = target.getSchema();
        if (schema.isEmpty()) {
            return;
        }
        var uniquePaths = index(
                schema.getUniqueConstraints(),
                String.format("$.targets.relationships[%d].schema.unique_constraints", index),
                (constraint) -> constraint.getProperties());
        var keyPaths = index(
                schema.getKeyConstraints(),
                String.format("$.targets.relationships[%d].schema.key_constraints", index),
                (constraint) -> constraint.getProperties());
        recordRedundancies(
                String.format("$.targets.relationships[%d].schema", index), redundancies(uniquePaths, keyPaths));
    }
}
