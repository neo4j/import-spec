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
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDanglingLabelInTextIndexValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "DANG-015";

    private final Map<String, String> invalidPaths;

    public NoDanglingLabelInTextIndexValidator() {
        this.invalidPaths = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var basePath = String.format("$.targets.nodes[%d].schema.text_indexes", index);
        var labels = target.getLabels();
        var textIndexes = target.getSchema().getTextIndexes();
        for (int i = 0; i < textIndexes.size(); i++) {
            var label = textIndexes.get(i).getLabel();
            if (!labels.contains(label)) {
                var path = String.format("%s[%d].label", basePath, i);
                invalidPaths.put(path, label);
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((path, bogusLabel) -> builder.addError(
                path, ERROR_CODE, String.format("%s \"%s\" is not part of the defined labels", path, bogusLabel)));
        return !invalidPaths.isEmpty();
    }
}
