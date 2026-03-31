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
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoNullDimensionOnVectorTargetTypesValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "TYPE-003";
    private static final int MIN_DIMENSION = 1;
    private static final int MAX_DIMENSION = 4096;

    private final Map<String, String> invalidPaths;

    public NoNullDimensionOnVectorTargetTypesValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        visitEntity(index, "nodes", target);
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        visitEntity(index, "relationships", target);
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((path, message) -> builder.addError(path, ERROR_CODE, message));
        return !invalidPaths.isEmpty();
    }

    private void visitEntity(int index, String group, EntityTarget target) {
        var mappings = target.getProperties();
        for (int i = 0; i < mappings.size(); i++) {
            PropertyMapping mapping = mappings.get(i);
            PropertyType type = mapping.getTargetPropertyType();
            if (type == null || !type.getName().isVector()) {
                continue;
            }
            var path = String.format("$.targets.%s[%d].properties[%d].target_property_type.dimension", group, index, i);
            Integer dimension = type.getDimension();
            if (dimension == null) {
                invalidPaths.put(
                        path,
                        String.format(
                                "%s vector type \"%s\" must specify a dimension between %d and %d",
                                path, type.getName().name(), MIN_DIMENSION, MAX_DIMENSION));
            }
        }
    }
}
