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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingPropertyInVectorIndexValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-022";

    private final Map<String, String> invalidPaths;

    public NoDanglingPropertyInVectorIndexValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        if (schema == null) {
            return;
        }
        var basePath = String.format("$.targets.nodes[%d].schema.vector_indexes", index);
        var properties = propertiesOf(target);
        var vectorIndexes = schema.getVectorIndexes();
        for (int i = 0; i < vectorIndexes.size(); i++) {
            var vectorIndex = vectorIndexes.get(i);
            var property = vectorIndex.getProperty();
            if (!properties.contains(property)) {
                var path = String.format("%s[%d].property", basePath, i);
                invalidPaths.put(path, property);
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

    private static Set<String> propertiesOf(EntityTarget target) {
        return target.getProperties().stream()
                .map(PropertyMapping::getTargetProperty)
                .collect(Collectors.toSet());
    }
}
