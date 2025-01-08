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
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedTargetPropertyValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DUPL-009";

    private final Map<String, Duplicate<String>> duplicateProperties;

    public NoDuplicatedTargetPropertyValidator() {
        this.duplicateProperties = new LinkedHashMap<>();
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
        duplicateProperties.forEach((path, duplicate) -> builder.addError(
                path,
                ERROR_CODE,
                String.format(
                        "%s \"%s\" must be defined only once but found %d occurrences",
                        path, duplicate.getValue(), duplicate.getCount())));
        return !duplicateProperties.isEmpty();
    }

    private void visitEntity(int index, EntityTarget target) {
        var properties = target.getProperties().stream()
                .map(PropertyMapping::getTargetProperty)
                .collect(Collectors.toList());
        var group = target instanceof NodeTarget ? "nodes" : "relationships";
        Duplicate.findDuplicates(properties).forEach(duplicate -> {
            var i = properties.indexOf(duplicate.getValue());
            var path = String.format("$.targets.%s[%d].properties[%d].target_property", group, index, i);
            duplicateProperties.put(path, duplicate);
        });
    }
}
