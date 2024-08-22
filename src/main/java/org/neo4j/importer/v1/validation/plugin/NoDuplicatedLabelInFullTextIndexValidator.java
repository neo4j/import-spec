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
import org.neo4j.importer.v1.targets.NodeFullTextIndex;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedLabelInFullTextIndexValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DUPL-010";

    private final Map<String, Duplicate<String>> duplicateLabels;

    public NoDuplicatedLabelInFullTextIndexValidator() {
        duplicateLabels = new LinkedHashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDanglingLabelInFullTextIndexValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var schema = target.getSchema();

        if (schema == null) {
            return;
        }

        var fTIndexes = schema.getFullTextIndexes();

        var basePath = String.format("$.targets.nodes[%d].schema.fulltext_indexes", index);

        for (int i = 0; i < fTIndexes.size(); i++) {
            NodeFullTextIndex textIndex = fTIndexes.get(i);

            int arrayIndex = i;
            Duplicate.findDuplicates(textIndex.getLabels()).forEach(duplicate -> {
                var path = String.format("%s[%d].label", basePath, arrayIndex);
                duplicateLabels.put(path, duplicate);
            });
        }
    }

    @Override
    public boolean report(Builder builder) {
        duplicateLabels.forEach((path, duplicate) -> builder.addError(
                path,
                ERROR_CODE,
                String.format(
                        "%s \"%s\" must be defined at most once but %d occurrences were found",
                        path, duplicate.getValue(), duplicate.getCount())));
        return !duplicateLabels.isEmpty();
    }
}
