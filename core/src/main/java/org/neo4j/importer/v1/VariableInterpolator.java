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

import static com.jayway.jsonpath.Configuration.builder;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.Options.Builder.VARIABLE_NAME_PATTERN;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.neo4j.importer.v1.ImportSpecificationDeserializer.Options;

class VariableInterpolator {

    private final ObjectMapper mapper;

    private final Configuration jsonPathConfig;

    public VariableInterpolator() {
        this.mapper = new ObjectMapper();
        this.jsonPathConfig = builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .build();
    }

    public JsonNode interpolate(JsonNode json, Options options) {
        var interpolationPaths = options.getInterpolationPaths();
        if (interpolationPaths.isEmpty()) {
            return json;
        }

        var variables = options.getVariables();
        var context = JsonPath.using(jsonPathConfig).parse(json);
        interpolationPaths.forEach(
                path -> context.map(path, ((currentValue, config) -> interpolate(currentValue, variables))));
        return context.json();
    }

    private Object interpolate(Object value, Map<String, Object> variables) {
        if (value instanceof ValueNode) {
            var jsonNode = (ValueNode) value;
            var variableNames = variables.keySet();
            var textValue = jsonNode.asText();
            var variableName = findExactlyMatchingVariable(variableNames, textValue);
            if (variableName.isPresent()) {
                var variableValue = variables.get(variableName.get());
                return toJsonNode(variableValue);
            }
            if (value instanceof TextNode) {
                // TODO: constant
                var newText = Pattern.compile(String.format("\\$(%s)", VARIABLE_NAME_PATTERN))
                        .matcher(textValue)
                        .replaceAll(match -> {
                            var name = match.group(1);
                            var variableValue = variables.getOrDefault(name, match.group(0));
                            if (variableValue instanceof String) {
                                return (String) variableValue;
                            }
                            try {
                                return mapper.writeValueAsString(variableValue);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        });
                return newText.equals(textValue) ? jsonNode : toJsonNode(newText);
            }
        }
        return value;
    }

    private Optional<String> findExactlyMatchingVariable(Set<String> variableNames, String uninterpolatedValue) {
        return variableNames.stream()
                .filter(name -> {
                    var variableReference = String.format("$%s", name);
                    return uninterpolatedValue.equals(variableReference);
                })
                .findFirst();
    }

    private static JsonNode toJsonNode(Object value) {
        if (value instanceof Boolean) {
            return JsonNodeFactory.instance.booleanNode((Boolean) value);
        }
        if (value instanceof Integer) {
            return JsonNodeFactory.instance.numberNode((Integer) value);
        }
        if (value instanceof Long) {
            return JsonNodeFactory.instance.numberNode((Long) value);
        }
        if (value instanceof Double) {
            return JsonNodeFactory.instance.numberNode((Double) value);
        }
        if (value instanceof Float) {
            return JsonNodeFactory.instance.numberNode((Float) value);
        }
        if (value instanceof BigDecimal) {
            return JsonNodeFactory.instance.numberNode((BigDecimal) value);
        }
        if (value instanceof BigInteger) {
            return JsonNodeFactory.instance.numberNode((BigInteger) value);
        }
        if (value instanceof Number) {
            return JsonNodeFactory.instance.numberNode(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            return JsonNodeFactory.instance.textNode((String) value);
        }
        throw new IllegalArgumentException("Unsupported variable value type: " + value.getClass());
    }
}
