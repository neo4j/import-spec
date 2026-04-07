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
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.*;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoNullDimensionOnVectorTypeConstraintValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "TYPE-003";

    private final Map<String, String> invalidPaths;

    public NoNullDimensionOnVectorTypeConstraintValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var basePath = String.format("$.targets.nodes[%d].schema.type_constraints", index);
        var typeByProperty = target.getProperties().stream()
                .filter(p -> p.getTargetPropertyType() != null)
                .collect(Collectors.toMap(PropertyMapping::getTargetProperty, PropertyMapping::getTargetPropertyType));
        var typeConstraints = target.getSchema().getTypeConstraints();
        for (int i = 0; i < typeConstraints.size(); i++) {
            var propertyType = typeByProperty.get(typeConstraints.get(i).getProperty());
            if (propertyType == null || !propertyType.getName().isVector()) {
                continue;
            }
            if (propertyType.getDimension() == null) {
                invalidPaths.put(
                        String.format("%s[%d]", basePath, i),
                        "dimension of referenced property cannot be null for vector type constraint");
            }
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var basePath = String.format("$.targets.relationships[%d].schema.type_constraints", index);
        var typeByProperty = target.getProperties().stream()
                .filter(p -> p.getTargetPropertyType() != null)
                .collect(Collectors.toMap(PropertyMapping::getTargetProperty, PropertyMapping::getTargetPropertyType));
        var typeConstraints = target.getSchema().getTypeConstraints();
        for (int i = 0; i < typeConstraints.size(); i++) {
            var propertyType = typeByProperty.get(typeConstraints.get(i).getProperty());
            if (propertyType == null || !propertyType.getName().isVector()) {
                continue;
            }
            if (propertyType.getDimension() == null) {
                invalidPaths.put(
                        String.format("%s[%d]", basePath, i),
                        "dimension of referenced property cannot be null for vector type constraint");
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((path, message) -> builder.addError(path, ERROR_CODE, message));
        return !invalidPaths.isEmpty();
    }
}
