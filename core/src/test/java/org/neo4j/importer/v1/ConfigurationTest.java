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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfigurationTest {

    @Test
    void returns_matching_element() {
        Configuration configuration = new Configuration(Map.of("a", 1));

        Optional<Integer> result = configuration.get(Integer.class, "a");

        assertThat(result).contains(1);
    }

    @Test
    void returns_element_matching_alternative_name() {
        Configuration configuration = new Configuration(Map.of("a", 1));

        Optional<Integer> result = configuration.get(Integer.class, "alpha", "α", "a");

        assertThat(result).contains(1);
    }

    @Test
    void does_not_return_element_if_no_names_match() {
        Configuration configuration = new Configuration(Map.of("a", 1));

        Optional<Integer> result = configuration.get(Integer.class, "alpha", "α", "β");

        assertThat(result).isEmpty();
    }

    @Test
    void does_not_return_element_if_type_does_not_match() {
        Configuration configuration = new Configuration(Map.of("a", 1));

        Optional<String> result = configuration.get(String.class, "a");

        assertThat(result).isEmpty();
    }
}
