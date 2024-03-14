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

import static org.neo4j.importer.v1.validation.plugin.Duplicate.findDuplicates;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.TextSource;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedSourceHeaderColumn implements SpecificationValidator {
    private static final String ERROR_CODE = "DUPL-002";

    private final Map<String, Duplicate<String>> sourcePathToDuplicates;

    public NoDuplicatedSourceHeaderColumn() {
        sourcePathToDuplicates = new LinkedHashMap<>();
    }

    @Override
    public void visitSource(int index, Source source) {
        if (!(source instanceof TextSource)) {
            return;
        }
        TextSource textSource = (TextSource) source;
        List<String> header = textSource.getHeader();
        if (header == null) {
            return;
        }
        String sourcePath = String.format("$.sources[%d].header", index);
        findDuplicates(header).forEach(duplicate -> sourcePathToDuplicates.put(sourcePath, duplicate));
    }

    @Override
    public boolean report(Builder builder) {
        if (sourcePathToDuplicates.isEmpty()) {
            return false;
        }
        sourcePathToDuplicates.forEach((sourcePath, duplicate) -> {
            builder.addError(
                    sourcePath,
                    ERROR_CODE,
                    String.format(
                            "%s defines column \"%s\" %d times, it must be defined at most once",
                            sourcePath, duplicate.getValue(), duplicate.getCount()));
        });
        return true;
    }
}
