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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.TextSource;
import org.neo4j.importer.v1.targets.Aggregation;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.SourceTransformations;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class InvalidTextColumnMappingValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "MCOL-002";

    private final Map<String, Set<String>> sourceFields;

    private final Map<String, String> invalidFields;

    public InvalidTextColumnMappingValidator() {
        this.sourceFields = new HashMap<>();
        this.invalidFields = new LinkedHashMap<>();
    }

    @Override
    public void visitSource(int index, Source source) {
        if (source instanceof TextSource) {
            sourceFields.put(source.getName(), new HashSet<>(((TextSource) source).getHeader()));
        }
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
        invalidFields.forEach((path, field) -> {
            builder.addError(
                    path,
                    ERROR_CODE,
                    String.format(
                            "%s field \"%s\" is neither defined in the target's text source nor its source transformations",
                            path, field));
        });
        return !invalidFields.isEmpty();
    }

    private void visitEntity(int index, EntityTarget target) {
        var source = target.getSource();
        if (!sourceFields.containsKey(source)) {
            return;
        }
        var sourceFields = this.sourceFields.get(source);
        var aggregatedFields = getAggregatedFields(target);
        var mappings = target.getProperties();
        var group = target instanceof NodeTarget ? "nodes" : "relationships";
        for (int i = 0; i < mappings.size(); i++) {
            var path = String.format("$.targets.%s[%d].properties[%d].source_field", group, index, i);
            String sourceField = mappings.get(i).getSourceField();
            if (!sourceFields.contains(sourceField) && !aggregatedFields.contains(sourceField)) {
                invalidFields.put(path, sourceField);
            }
        }
    }

    private static Set<String> getAggregatedFields(EntityTarget target) {
        var aggregatedFields = new HashSet<String>();
        SourceTransformations sourceTransformations = target.getSourceTransformations();
        if (sourceTransformations != null) {
            sourceTransformations.getAggregations().stream()
                    .map(Aggregation::getFieldName)
                    .forEach(aggregatedFields::add);
        }
        return aggregatedFields;
    }
}
