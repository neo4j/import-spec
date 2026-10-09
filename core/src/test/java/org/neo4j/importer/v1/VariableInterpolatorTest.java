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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.importer.v1.ImportSpecificationDeserializer.Options;

class VariableInterpolatorTest {

    private final YAMLMapper mapper = new YAMLMapper();

    private final VariableInterpolator interpolator = new VariableInterpolator();

    @ParameterizedTest
    @MethodSource("interpolated_values")
    void interpolates_values(Object value) throws Exception {
        // TODO: yaml
        var json = "{\"key\": \"$value\"}";

        var result = interpolator.interpolate(
                jsonNode(json),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(Map.of("value", value))
                        .build());

        var actualValue = mapper.convertValue(result.get("key"), value.getClass());
        assertThat(actualValue)
                .overridingErrorMessage(
                        "Variable interpolation failed: values don't match (expected: %s, got: %s)", value, actualValue)
                .isEqualTo(value);
    }

    @Test
    void interpolates_substring_values() throws Exception {
        var json = "{\"key\": \"a $value !!\"}";

        JsonNode result = interpolator.interpolate(
                jsonNode(json),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(Map.of("val", "ignored", "value", "word"))
                        .build());

        assertThat(result.get("key").asText()).isEqualTo("a word !!");
    }

    @Test
    void interpolates_multiple_values() throws Exception {
        var json = "{\"key\": \"a $value1 and $val !!\"}";

        JsonNode result = interpolator.interpolate(
                jsonNode(json),
                Options.builder()
                        .enableInterpolation()
                        .allowInterpolationAt("$.key")
                        .interpolationVariables(Map.of("val", true, "value1", 42))
                        .build());

        assertThat(result.get("key").asText()).isEqualTo("a 42 and true !!");
    }

    private JsonNode jsonNode(String json) throws Exception {
        return mapper.readTree(json);
    }

    private static Stream<Arguments> interpolated_values() {
        return Stream.of(
                Arguments.of(true), Arguments.of("string"), Arguments.of(42), Arguments.of(42f), Arguments.of(42.0d));
    }
}
