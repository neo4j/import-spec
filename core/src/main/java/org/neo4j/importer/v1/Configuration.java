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
package org.neo4j.importer.v1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class Configuration implements Serializable {

    private final Map<String, Object> settings;

    public Configuration(Map<String, Object> settings) {
        this.settings = settings != null ? settings : Collections.emptyMap();
    }

    public <T> Optional<T> get(Class<T> type, String name, String... alternativeNames) {
        if (settings.isEmpty()) {
            return Optional.empty();
        }
        return Stream.concat(Stream.of(name), Arrays.stream(alternativeNames))
                .map(key -> getElement(type, key))
                .dropWhile(Optional::isEmpty)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private <T> Optional<T> getElement(Class<T> type, String name) {
        return settings.containsKey(name) ? Optional.of(type.cast(settings.get(name))) : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configuration that = (Configuration) o;
        return Objects.equals(settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settings);
    }

    @Override
    public String toString() {
        return "Configuration{" + "settings=" + settings + '}';
    }
}
