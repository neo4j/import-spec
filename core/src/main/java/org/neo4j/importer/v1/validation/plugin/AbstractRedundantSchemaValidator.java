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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

// Shared machinery for validators that report a redundancy when the same label/property pattern is
// covered by two different kinds of schema definition (e.g. a key constraint and a range index).
// Subclasses extract each kind into a Map keyed by the comparison pattern (via {@link #index} or
// {@link #indexMulti}), intersect them with {@link #redundancies}, and record the result with
// {@link #recordRedundancies}.
abstract class AbstractRedundantSchemaValidator implements SpecificationValidator {

    private final String errorCode;
    private final String redundancyDescription;
    private final Map<String, List<List<String>>> invalidPaths;

    protected AbstractRedundantSchemaValidator(String errorCode, String redundancyDescription) {
        this.errorCode = errorCode;
        this.redundancyDescription = redundancyDescription;
        this.invalidPaths = new LinkedHashMap<>();
    }

    // Indexes each definition by the pattern returned by {@code keyOf}, mapping it to its JSON path.
    protected static <T, K> Map<K, List<String>> index(List<T> definitions, String basePath, Function<T, K> keyOf) {
        return indexMulti(definitions, basePath, definition -> List.of(keyOf.apply(definition)));
    }

    // Like {@link #index}, but each definition may contribute several patterns (e.g. a composite key
    // constraint that is redundant per individual property).
    protected static <T, K> Map<K, List<String>> indexMulti(
            List<T> definitions, String basePath, Function<T, Collection<K>> keysOf) {
        var paths = new LinkedHashMap<K, List<String>>();
        for (int i = 0; i < definitions.size(); i++) {
            var path = String.format("%s[%d]", basePath, i);
            for (var key : keysOf.apply(definitions.get(i))) {
                paths.computeIfAbsent(key, (ignored) -> new ArrayList<>(1)).add(path);
            }
        }
        return paths;
    }

    // A redundancy exists only when the same pattern is covered by both maps. Two definitions of the
    // same kind sharing a pattern are not reported here.
    protected static <K> List<List<String>> redundancies(Map<K, List<String>> left, Map<K, List<String>> right) {
        var redundancies = new ArrayList<List<String>>();
        left.forEach((pattern, leftDefinitions) -> {
            var rightDefinitions = right.get(pattern);
            if (rightDefinitions != null) {
                var redundantDefinitions = new ArrayList<String>(leftDefinitions.size() + rightDefinitions.size());
                redundantDefinitions.addAll(leftDefinitions);
                redundantDefinitions.addAll(rightDefinitions);
                redundancies.add(redundantDefinitions);
            }
        });
        return redundancies;
    }

    protected void recordRedundancies(String schemaPath, List<List<String>> redundancies) {
        if (!redundancies.isEmpty()) {
            invalidPaths.put(schemaPath, redundancies);
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach((schemaPath, redundancies) -> redundancies.forEach((redundantDefinitions) -> {
            String redundantDefs = redundantDefinitions.stream()
                    .map(def -> def.replace(schemaPath + ".", ""))
                    .collect(Collectors.joining(", "));
            builder.addError(
                    schemaPath,
                    errorCode,
                    String.format("%s defines redundant %s: %s", schemaPath, redundancyDescription, redundantDefs));
        }));
        return !invalidPaths.isEmpty();
    }
}
