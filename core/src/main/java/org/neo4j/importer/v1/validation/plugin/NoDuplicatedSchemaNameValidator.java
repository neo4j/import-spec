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
    private final List<SchemaElement> elements = new ArrayList<>();

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
        addConstraints(basePath, target, target.getSchema());
        addIndexes(basePath, target, target.getSchema());
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        String basePath = String.format("$.targets.relationships[%d].schema", index);
        addConstraints(basePath, target, target.getSchema());
        addIndexes(basePath, target, target.getSchema());
    }

    @Override
    public boolean report(Builder builder) {
        var duplicates = elements.stream().collect(Collectors.groupingBy(SchemaElement::getName)).entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toList());

        duplicates.forEach(dup -> builder.addError(
                dup.getValue().get(0).getPath(),
                ERROR_CODE,
                String.format(
                        "Constraint or index name \"%s\" must be defined at most once but %d occurrences were found.",
                        dup.getKey(), dup.getValue().size())));
        return !duplicates.isEmpty();
    }

    private void addConstraints(String basePath, EntityTarget target, Schema schema) {
        addElements(basePath, target, schema.getKeyConstraints(), "key_constraints");
        addElements(basePath, target, schema.getUniqueConstraints(), "unique_constraints");
        addElements(basePath, target, schema.getExistenceConstraints(), "existence_constraints");
        addElements(basePath, target, schema.getTypeConstraints(), "type_constraints");
    }

    private void addIndexes(String basePath, EntityTarget target, Schema schema) {
        addElements(basePath, target, schema.getRangeIndexes(), "range_indexes");
        addElements(basePath, target, schema.getTextIndexes(), "text_indexes");
        addElements(basePath, target, schema.getPointIndexes(), "point_indexes");
        addElements(basePath, target, schema.getFullTextIndexes(), "fulltext_indexes");
        addElements(basePath, target, schema.getVectorIndexes(), "vector_indexes");
    }

    private <T> void addElements(String basePath, EntityTarget target, List<? extends T> list, String type) {
        for (int i = 0; i < list.size(); i++) {
            T item = list.get(i);
            String path = String.format("%s.%s[%d]", basePath, type, i);
            if (item instanceof Constraint) {
                elements.add(new ConstraintElement(path, (Constraint) item));
            } else if (item instanceof Index) {
                elements.add(new IndexElement(path, (Index) item));
            } else {
                throw new IllegalArgumentException("Unknown schema item type: " + item.getClass());
            }
        }
    }

    private interface SchemaElement {
        String getPath();

        String getName();
    }

    private static class ConstraintElement implements SchemaElement {
        private final String path;
        private final Constraint constraint;

        ConstraintElement(String path, Constraint constraint) {
            this.path = path;
            this.constraint = constraint;
        }

        public String getName() {
            return constraint.getName();
        }

        public String getPath() {
            return path;
        }
    }

    private static class IndexElement implements SchemaElement {
        private final String path;
        private final Index index;

        IndexElement(String path, Index index) {
            this.path = path;
            this.index = index;
        }

        public String getName() {
            return index.getName();
        }

        public String getPath() {
            return path;
        }
    }
}
