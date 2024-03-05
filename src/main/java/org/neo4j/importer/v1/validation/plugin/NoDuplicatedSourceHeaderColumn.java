/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.TextSource;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDuplicatedSourceHeaderColumn implements SpecificationValidator {
    private static final String ERROR_CODE = "DUPL-002";

    private final Map<String, DuplicatedHeader> sourcePathToDuplicates;

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
        getDuplicates(header).forEach(duplicate -> sourcePathToDuplicates.put(sourcePath, duplicate));
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
                            sourcePath, duplicate.getName(), duplicate.getCount()));
        });
        return true;
    }

    private static List<DuplicatedHeader> getDuplicates(List<String> header) {
        return header.stream().collect(groupingBy(identity(), counting())).entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> new DuplicatedHeader(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}

class DuplicatedHeader {
    private final String name;
    private final long count;

    public DuplicatedHeader(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        DuplicatedHeader that = (DuplicatedHeader) object;
        return count == that.count && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, count);
    }
}
