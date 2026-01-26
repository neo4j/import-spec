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
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoInvalidCharsInTypeAndLabelValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "CHAR-001";

    private static final char COLON = ':';
    private static final char EQUAL_SIGN = '=';

    private final Map<String, String> invalidLabelsAndType = new LinkedHashMap<>();

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        for (int labelInd = 0; labelInd < target.getLabels().size(); labelInd++) {
            var label = target.getLabels().get(labelInd);
            if (label.chars().anyMatch(c -> c == COLON || c == EQUAL_SIGN)) {
                invalidLabelsAndType.put(String.format("$.targets.nodes[%d].labels[%d]", index, labelInd), label);
            }
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        if (target.getType().chars().anyMatch(c -> c == EQUAL_SIGN)) {
            invalidLabelsAndType.put(String.format("$.targets.relationships[%d].type", index), target.getType());
        }
    }

    @Override
    public boolean report(SpecificationValidationResult.Builder builder) {
        invalidLabelsAndType.forEach((path, labelOrType) -> builder.addError(
                path, ERROR_CODE, String.format("%s \"%s\" contains invalid character", path, labelOrType)));
        return !invalidLabelsAndType.isEmpty();
    }
}
