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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.importer.v1.ImportSpecificationDeserializer.Options;
import org.neo4j.importer.v1.validation.InvalidDeserializerOptionsException;

class ImportSpecificationDeserializerOptionsTest {

    @Test
    void defines_sensible_defaults() {
        var options = Options.builder().build();

        assertThat(options.getInterpolationPaths()).isEmpty();
        assertThat(options.getNeo4jDistribution()).isEmpty();
        assertThat(options.getVariables()).isEmpty();
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    class Interpolation {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "valid",
                    "still_valid",
                    "still_valid12",
                    "still_valid_",
                    "st1ll_v4l1d",
                    "st1ll_v4l1d",
                    "stıll_valıd",
                    "stıll_valıd",
                    "用户_५६७",
                    "用户_५६७",
                    "ب_١٢٣",
                })
        void accepts_valid_interpolation_variable_names(String variableName) {
            assertThatCode(() -> ImportSpecificationDeserializer.Options.builder()
                            .interpolationVariables(Map.of(variableName, "a value"))
                            .build())
                    .doesNotThrowAnyException();
        }

        @Test
        void rejects_null_interpolation_variable_names() {
            var variables =
                    ImportSpecificationDeserializerOptionsTest.<String, Object>nullFriendlyMapOf(null, "a value");
            assertThatThrownBy(() -> ImportSpecificationDeserializer.Options.builder()
                            .interpolationVariables(variables)
                            .build())
                    .isInstanceOf(InvalidDeserializerOptionsException.class)
                    .hasMessageContaining("Variable name cannot be null");
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "",
                    " ",
                    "with space",
                    "with\nnewline",
                    "with\ttab",
                    "with\rreturn",
                    "_invalid",
                    "1valid",
                })
        void rejects_invalid_interpolation_variable_names(String variableName) {
            assertThatThrownBy(() -> ImportSpecificationDeserializer.Options.builder()
                            .interpolationVariables(Map.of(variableName, "a value"))
                            .build())
                    .isInstanceOf(InvalidDeserializerOptionsException.class)
                    .hasMessageContaining(String.format(
                            "The name of variable '%s' must start with a letter, optionally followed by _ and/or other letters/numbers",
                            variableName));
        }

        @ParameterizedTest
        @MethodSource("valid_variable_values")
        void accepts_valid_interpolation_variable_values(Object variableValue) {
            assertThatCode(() -> ImportSpecificationDeserializer.Options.builder()
                            .interpolationVariables(Map.of("referee_checks_var", variableValue))
                            .build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @MethodSource("invalid_variable_values")
        void rejects_invalid_interpolation_variable_values(Object variableValue) {
            assertThatThrownBy(() -> ImportSpecificationDeserializer.Options.builder()
                            .interpolationVariables(Map.of("referee_checks_var", variableValue))
                            .build())
                    .isInstanceOf(InvalidDeserializerOptionsException.class)
                    .hasMessageContaining(String.format(
                            "The value of variable 'referee_checks_var' can only be a string, number or boolean, found: %s",
                            variableValue.getClass()));
        }

        @Test
        void rejects_options_if_interpolation_is_disabled_and_extra_paths_are_configured() {
            assertThatThrownBy(() -> ImportSpecificationDeserializer.Options.builder()
                            //                            .disableInterpolation()
                            .allowInterpolationAt("$.sources[*].name")
                            .allowInterpolationAt("$.actions[*].query")
                            .interpolationVariables(Map.of("a", "variable"))
                            .build())
                    .isInstanceOf(InvalidDeserializerOptionsException.class)
                    .hasMessageContaining(
                            "Interpolation is disabled but additional interpolation paths are configured. "
                                    + "Enable interpolation or remove those paths");
        }

        @Test
        void rejects_options_if_no_interpolation_variable_is_added_and_extra_paths_are_configured() {
            assertThatThrownBy(() -> ImportSpecificationDeserializer.Options.builder()
                            .enableInterpolation()
                            .allowInterpolationAt("$.sources[*].name")
                            .build())
                    .isInstanceOf(InvalidDeserializerOptionsException.class)
                    .hasMessageContaining(
                            "Additional interpolation paths are configured but no interpolation variables are defined. "
                                    + "Define variables or remove these paths");
        }

        private Stream<Arguments> valid_variable_values() {
            return Stream.of(
                    Arguments.of(true),
                    Arguments.of(false),
                    Arguments.of(42),
                    Arguments.of(42f),
                    Arguments.of(42d),
                    Arguments.of("string"));
        }

        private Stream<Arguments> invalid_variable_values() {
            return Stream.of(
                    Arguments.of(List.of(1)),
                    Arguments.of(List.of(1f)),
                    Arguments.of(List.of(1d)),
                    Arguments.of(List.of(false)),
                    Arguments.of(List.of("string")),
                    Arguments.of(List.of(List.of(1))),
                    Arguments.of(List.of(Map.of("key", 1))),
                    Arguments.of(Map.of("key", "value")),
                    Arguments.of(Map.of("key", List.of("value"))),
                    Arguments.of(new Object()));
        }
    }

    private static <K, V> Map<K, V> nullFriendlyMapOf(K key, V value) {
        var variables = new HashMap<K, V>(1);
        variables.put(key, value);
        return variables;
    }
}
