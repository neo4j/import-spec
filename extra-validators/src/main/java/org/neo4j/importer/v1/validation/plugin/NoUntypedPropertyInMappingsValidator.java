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
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

/**
 * For <code>data-importer</code> graph models converted to import-spec we have a requirement that all mapping targets must contain types.
 */
public class NoUntypedPropertyInMappingsValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "TYPE-002";

    private final Map<String, String> invalidProperties;

    public NoUntypedPropertyInMappingsValidator() {
        this.invalidProperties = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        visitEntity(index, target);
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        visitEntity(index, target);
    }

    @Override
    public boolean report(Builder builder) {
        invalidProperties.forEach((path, prop) -> builder.addError(
                path, ERROR_CODE, String.format("%s \"%s\" refers to an untyped property", path, prop)));
        return !invalidProperties.isEmpty();
    }

    private void visitEntity(int index, EntityTarget target) {
        var group = target instanceof NodeTarget ? "nodes" : "relationships";
        var propertyMappings = target.getProperties();

        for (int i = 0; i < propertyMappings.size(); i++) {
            var propertyMapping = propertyMappings.get(i);
            if (propertyMapping.getTargetPropertyType() == null) {
                var path = String.format("$.targets.%s[%d].properties[%d].target_property_type", group, index, i);
                invalidProperties.put(path, propertyMapping.getTargetProperty());
            }
        }
    }
}
