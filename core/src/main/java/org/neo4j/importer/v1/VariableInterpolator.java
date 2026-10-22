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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.ImportSpecificationDeserializer.Options;

class VariableInterpolator {

    private final ObjectMapper mapper;

    private final Configuration jsonPathConfig;

    public VariableInterpolator() {
        this.mapper = new ObjectMapper();
        this.jsonPathConfig = builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    public JsonNode interpolate(JsonNode json, Options options) {
        var interpolationPaths = options.getInterpolationPaths();
        if (interpolationPaths.isEmpty()) {
            return json;
        }

        var variables = options.getVariables();
        var variableNames = variables.keySet().stream().map(Pattern::quote).collect(Collectors.joining("|"));
        var variablePattern = Pattern.compile("\\$(" + variableNames + ")(?![\\p{L}\\p{Nd}_])");
        var context = JsonPath.using(jsonPathConfig).parse(json);
        interpolationPaths.forEach(path ->
                context.map(path, ((currentValue, config) -> interpolate(currentValue, variables, variablePattern))));
        return context.json();
    }

    private Object interpolate(Object value, Map<String, Object> variables, Pattern variablePattern) {
        if (value instanceof NullNode) {
            return value;
        }
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
                var newText = variablePattern.matcher(textValue).replaceAll(match -> {
                    var name = match.group(1);
                    var variableValue = variables.get(name);
                    if (variableValue instanceof String) {
                        return Matcher.quoteReplacement((String) variableValue);
                    }
                    try {
                        return Matcher.quoteReplacement(mapper.writeValueAsString(variableValue));
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

    private JsonNode toJsonNode(Object value) {
        if (!(value instanceof Boolean) && !(value instanceof Number) && !(value instanceof String)) {
            throw new IllegalArgumentException("Unsupported variable value type: " + value.getClass());
        }
        return mapper.valueToTree(value);
    }
}
