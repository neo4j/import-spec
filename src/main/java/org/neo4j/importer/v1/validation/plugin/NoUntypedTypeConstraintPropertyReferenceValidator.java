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

public class NoUntypedTypeConstraintPropertyReferenceValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "TYPE-001";

    private final Map<String, String> invalidPaths;

    public NoUntypedTypeConstraintPropertyReferenceValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDanglingTypeConstraintPropertyReferenceValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();
        if (schema == null) {
            return;
        }
        var basePath = String.format("$.targets.nodes[%d].schema.type_constraints", index);
        var properties = typedPropertiesOf(target);
        var typeConstraints = schema.getTypeConstraints();
        for (int i = 0; i < typeConstraints.size(); i++) {
            var property = typeConstraints.get(i).getProperty();
            if (!properties.contains(property)) {
                var path = String.format("%s[%d].property", basePath, i);
                invalidPaths.put(path, property);
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((path, bogusProperty) -> builder.addError(
                path, ERROR_CODE, String.format("%s \"%s\" refers to an untyped property", path, bogusProperty)));
        return !invalidPaths.isEmpty();
    }

    private static Set<String> typedPropertiesOf(EntityTarget target) {
        return target.getProperties().stream()
                .filter(p -> p.getTargetPropertyType() != null)
                .map(PropertyMapping::getTargetProperty)
                .collect(Collectors.toSet());
    }
}
