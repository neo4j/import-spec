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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.*;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

// within a single target, two constraints (or two indexes) of the same kind that cover the exact same definition
// (label(s)/properties/options, but a different name) are duplicates: they describe the same schema element declared
// twice.
// note: property order is significant (composite indexes are order-sensitive), so the same properties in a different
// order are not considered duplicates. Definitions declared under the same name are left to
// NoDuplicatedSchemaNameValidator.
public class NoDuplicatedSchemaDefinitionValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DUPL-012";

    private final List<Definition> definitions = new ArrayList<>();

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var basePath = String.format("$.targets.nodes[%d].schema", index);
        var schema = target.getSchema();
        collect(
                basePath,
                "type_constraints",
                schema.getTypeConstraints(),
                c -> signature(c.getLabel(), c.getProperty()));
        collect(
                basePath,
                "key_constraints",
                schema.getKeyConstraints(),
                c -> signature(c.getLabel(), c.getProperties(), c.getOptions()));
        collect(
                basePath,
                "unique_constraints",
                schema.getUniqueConstraints(),
                c -> signature(c.getLabel(), c.getProperties(), c.getOptions()));
        collect(
                basePath,
                "existence_constraints",
                schema.getExistenceConstraints(),
                c -> signature(c.getLabel(), c.getProperty()));
        collect(basePath, "range_indexes", schema.getRangeIndexes(), i -> signature(i.getLabel(), i.getProperties()));
        collect(
                basePath,
                "text_indexes",
                schema.getTextIndexes(),
                i -> signature(i.getLabel(), i.getProperty(), i.getOptions()));
        collect(
                basePath,
                "point_indexes",
                schema.getPointIndexes(),
                i -> signature(i.getLabel(), i.getProperty(), i.getOptions()));
        collect(
                basePath,
                "fulltext_indexes",
                schema.getFullTextIndexes(),
                i -> signature(i.getLabels(), i.getProperties(), i.getOptions()));
        collect(
                basePath,
                "vector_indexes",
                schema.getVectorIndexes(),
                i -> signature(i.getLabel(), i.getProperty(), i.getOptions()));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var basePath = String.format("$.targets.relationships[%d].schema", index);
        var schema = target.getSchema();
        collect(basePath, "type_constraints", schema.getTypeConstraints(), c -> signature(c.getProperty()));
        collect(
                basePath,
                "key_constraints",
                schema.getKeyConstraints(),
                c -> signature(c.getProperties(), c.getOptions()));
        collect(
                basePath,
                "unique_constraints",
                schema.getUniqueConstraints(),
                c -> signature(c.getProperties(), c.getOptions()));
        collect(basePath, "existence_constraints", schema.getExistenceConstraints(), c -> signature(c.getProperty()));
        collect(basePath, "range_indexes", schema.getRangeIndexes(), i -> signature(i.getProperties()));
        collect(basePath, "text_indexes", schema.getTextIndexes(), i -> signature(i.getProperty(), i.getOptions()));
        collect(basePath, "point_indexes", schema.getPointIndexes(), i -> signature(i.getProperty(), i.getOptions()));
        collect(
                basePath,
                "fulltext_indexes",
                schema.getFullTextIndexes(),
                i -> signature(i.getProperties(), i.getOptions()));
        collect(basePath, "vector_indexes", schema.getVectorIndexes(), i -> signature(i.getProperty(), i.getOptions()));
    }

    @Override
    public boolean report(Builder builder) {
        var groups = definitions.stream()
                .collect(Collectors.groupingBy(
                        definition -> List.of(definition.groupPath(), definition.signature()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        var reported = false;
        for (var group : groups.values()) {
            var distinctNames = group.stream().map(Definition::name).distinct().count();
            if (distinctNames < 2) {
                // a single name (possibly repeated) is a name clash, reported by NoDuplicatedSchemaNameValidator
                continue;
            }
            reported = true;
            var names = group.stream()
                    .map(definition -> '"' + definition.name() + '"')
                    .collect(Collectors.joining(", "));
            var first = group.get(0);
            builder.addError(
                    first.path(),
                    ERROR_CODE,
                    String.format(
                            "%s defines duplicate entries covering the same properties: %s", first.groupPath(), names));
        }
        return reported;
    }

    private <T> void collect(String basePath, String type, List<T> elements, Function<T, List<Object>> signatureFn) {
        var groupPath = String.format("%s.%s", basePath, type);
        for (int i = 0; i < elements.size(); i++) {
            var element = elements.get(i);
            var path = String.format("%s[%d]", groupPath, i);
            definitions.add(new Definition(groupPath, signatureFn.apply(element), path, name(element)));
        }
    }

    private static String name(Object element) {
        if (element instanceof Constraint) {
            return ((Constraint) element).getName();
        }
        if (element instanceof Index) {
            return ((Index) element).getName();
        }
        throw new IllegalArgumentException("Unknown schema element type: " + element.getClass());
    }

    // Arrays.asList (unlike List.of) tolerates the null options/labels some definitions carry
    private static List<Object> signature(Object... parts) {
        return Arrays.asList(parts);
    }

    private static final class Definition {
        private final String groupPath;
        private final List<Object> signature;
        private final String path;
        private final String name;

        Definition(String groupPath, List<Object> signature, String path, String name) {
            this.groupPath = groupPath;
            this.signature = signature;
            this.path = path;
            this.name = name;
        }

        String groupPath() {
            return groupPath;
        }

        List<Object> signature() {
            return signature;
        }

        String path() {
            return path;
        }

        String name() {
            return name;
        }
    }
}
