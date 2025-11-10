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

/**
 * {@link Configuration} is a simple, type-safe wrapper class over arbitrary top-level {@link ImportSpecification}
 * settings.
 */
public class Configuration implements Serializable {

    private final Map<String, Object> settings;

    public Configuration(Map<String, Object> settings) {
        this.settings = settings != null ? settings : Collections.emptyMap();
    }

    /**
     * Returns the setting matched by its primary name (or its alternate names), if it conforms to the provided type
     * @param type expected value type of the setting
     * @param name primary setting name
     * @param alternativeNames alternative setting names, if any
     * @return the setting value if it matches the provided parameters in an optional wrapper, or an empty optional
     * @param <T> expected type of the setting value
     */
    public <T> Optional<T> get(Class<T> type, String name, String... alternativeNames) {
        if (settings.isEmpty()) {
            return Optional.empty();
        }
        return Stream.concat(Stream.of(name), Arrays.stream(alternativeNames))
                .map(key -> get(type, key))
                .dropWhile(Optional::isEmpty)
                .flatMap(Optional::stream)
                .findFirst();
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

    private <T> Optional<T> get(Class<T> type, String name) {
        return settings.containsKey(name) ? Optional.ofNullable(safeCast(type, name)) : Optional.empty();
    }

    private <T> T safeCast(Class<T> type, String name) {
        var rawValue = settings.get(name);
        try {
            return type.cast(rawValue);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
