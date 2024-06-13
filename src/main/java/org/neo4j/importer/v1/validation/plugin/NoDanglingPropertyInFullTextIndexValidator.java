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

import static org.neo4j.importer.v1.validation.plugin.EntityTargets.propertiesOf;

import java.util.LinkedHashMap;
import java.util.Map;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingPropertyInFullTextIndexValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-020";

    private final Map<String, String> invalidPaths;

    public NoDanglingPropertyInFullTextIndexValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        if (schema == null) {
            return;
        }
        var basePath = String.format("$.targets.nodes[%d].schema.fulltext_indexes", index);
        var properties = propertiesOf(target);
        var fullTextIndexes = schema.getFullTextIndexes();
        for (int i = 0; i < fullTextIndexes.size(); i++) {
            var indexProperties = fullTextIndexes.get(i).getProperties();
            for (int j = 0; j < indexProperties.size(); j++) {
                var property = indexProperties.get(j);
                if (!properties.contains(property)) {
                    var path = String.format("%s[%d].properties[%d]", basePath, i, j);
                    invalidPaths.put(path, property);
                }
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((path, bogusProperty) -> builder.addError(
                path,
                ERROR_CODE,
                String.format("%s \"%s\" is not part of the property mappings", path, bogusProperty)));
        return !invalidPaths.isEmpty();
    }
}
