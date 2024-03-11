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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class Duplicate<T> {
    private final T value;
    private final long count;

    public Duplicate(T value, long count) {
        this.value = value;
        this.count = count;
    }

    public static <T> List<Duplicate<T>> findDuplicates(List<T> values) {
        return values.stream().collect(groupingBy(identity(), counting())).entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> new Duplicate<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public T getValue() {
        return value;
    }

    public long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Duplicate<?> that = (Duplicate<?>) object;
        return count == that.count && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, count);
    }
}
