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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.importer.v1.ImportSpecificationDeserializer.Options;

class VariableInterpolatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final VariableInterpolator interpolator = new VariableInterpolator();

    @ParameterizedTest
    @MethodSource("interpolated_values")
    void interpolates_values(Object value, JsonNode expectedValue) {
        var result = interpolator.interpolate(
                mapper.createObjectNode().put("key", "$value"),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(Map.of("value", value))
                        .build());

        assertThat(result.get("key"))
                .as("Interpolated value for %s", value.getClass().getSimpleName())
                .isEqualTo(expectedValue);
    }

    @Test
    void interpolates_substring_values() {
        var result = interpolator.interpolate(
                mapper.createObjectNode().put("key", "a $value !!"),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(Map.of("val", "ignored", "value", "word"))
                        .build());

        assertThat(result.get("key").asText()).isEqualTo("a word !!");
    }

    @Test
    void interpolates_multiple_values() {
        var result = interpolator.interpolate(
                mapper.createObjectNode().put("key", "a $value1 and $val !!"),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(Map.of("val", true, "value1", 42))
                        .build());

        assertThat(result.get("key").asText()).isEqualTo("a 42 and true !!");
    }

    @ParameterizedTest
    @MethodSource("named_variable_interpolations")
    void interpolates_only_defined_variables(String text, Map<String, Object> variables, String expected) {
        var result = interpolator.interpolate(
                mapper.createObjectNode().put("key", text),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(variables)
                        .build());

        assertThat(result.get("key").asText()).isEqualTo(expected);
    }

    private static Stream<Arguments> named_variable_interpolations() {
        return Stream.of(
                Arguments.of("$rows", Map.of("value", "word"), "$rows"),
                Arguments.of("$rows and $value", Map.of("value", "word"), "$rows and word"),
                Arguments.of(
                        "$val $value $val1 $val_ $valé $val١",
                        Map.of("val", "word"),
                        "word $value $val1 $val_ $valé $val١"),
                Arguments.of(
                        "$val $value $value1", Map.of("val", "short", "value", "long", "value1", 42), "short long 42"),
                Arguments.of("a $变量١ and $café !!", Map.of("变量١", "first", "café", "second"), "a first and second !!"),
                Arguments.of("a $value !!", Map.of("value", "C:\\data\\$rows"), "a C:\\data\\$rows !!"));
    }

    private static Stream<Arguments> interpolated_values() {
        var nodes = JsonNodeFactory.instance;
        return Stream.of(
                Arguments.of(true, nodes.booleanNode(true)),
                Arguments.of("string", nodes.textNode("string")),
                Arguments.of(42, nodes.numberNode(42)),
                Arguments.of(42L, nodes.numberNode(42L)),
                Arguments.of(42f, nodes.numberNode(42f)),
                Arguments.of(42.0d, nodes.numberNode(42.0d)),
                Arguments.of(new BigDecimal("42.125"), nodes.numberNode(new BigDecimal("42.125"))),
                Arguments.of(new BigInteger("9007199254740993"), nodes.numberNode(new BigInteger("9007199254740993"))),
                Arguments.of((byte) 42, nodes.numberNode(42)),
                Arguments.of((short) 42, nodes.numberNode(42)),
                Arguments.of(new AtomicInteger(42), nodes.numberNode(42)),
                Arguments.of(new AtomicLong(42), nodes.numberNode(42L)),
                Arguments.of(new AtomicLong(9_007_199_254_740_993L), nodes.numberNode(9_007_199_254_740_993L)));
    }
}
