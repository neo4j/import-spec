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

public class NoRedundantKeyConstraintAndRangeIndexValidator extends AbstractRedundantSchemaValidator {

    private static final String ERROR_CODE = "NRDC-003";

    public NoRedundantKeyConstraintAndRangeIndexValidator() {
        super(ERROR_CODE, "key constraint and range index");
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
        var rangeIndexPaths = index(
                schema.getRangeIndexes(),
                String.format("$.targets.nodes[%d].schema.range_indexes", index),
                (rangeIndex) -> new SchemaNodePattern(rangeIndex.getLabel(), rangeIndex.getProperties()));
        var keyPaths = index(
                schema.getKeyConstraints(),
                String.format("$.targets.nodes[%d].schema.key_constraints", index),
                (constraint) -> new SchemaNodePattern(constraint.getLabel(), constraint.getProperties()));
        recordRedundancies(String.format("$.targets.nodes[%d].schema", index), redundancies(rangeIndexPaths, keyPaths));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var schema = target.getSchema();
        if (schema.isEmpty()) {
            return;
        }
        var rangeIndexPaths = index(
                schema.getRangeIndexes(),
                String.format("$.targets.relationships[%d].schema.range_indexes", index),
                (rangeIndex) -> rangeIndex.getProperties());
        var keyPaths = index(
                schema.getKeyConstraints(),
                String.format("$.targets.relationships[%d].schema.key_constraints", index),
                (constraint) -> constraint.getProperties());
        recordRedundancies(
                String.format("$.targets.relationships[%d].schema", index), redundancies(rangeIndexPaths, keyPaths));
    }
}
