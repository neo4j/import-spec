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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionDeserializer;
import org.neo4j.importer.v1.actions.ActionProvider;
import org.neo4j.importer.v1.distribution.Neo4jDistribution;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceDeserializer;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.validation.ActionError;
import org.neo4j.importer.v1.validation.InvalidDeserializerOptionsException;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.Neo4jDistributionValidator;
import org.neo4j.importer.v1.validation.SourceError;
import org.neo4j.importer.v1.validation.SpecificationException;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;
import org.neo4j.importer.v1.validation.SpecificationValidators;
import org.neo4j.importer.v1.validation.UndeserializableActionException;
import org.neo4j.importer.v1.validation.UndeserializableSourceException;
import org.neo4j.importer.v1.validation.UndeserializableSpecificationException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

public class ImportSpecificationDeserializer {

    private static final JsonSchema SCHEMA = JsonSchemaFactory.getInstance(VersionFlag.V202012)
            .getSchema(ImportSpecificationDeserializer.class.getResourceAsStream("/spec.v1.json"));

    /**
     * Returns an instance of {@link ImportSpecification} based on the provided {@link Reader} content.<br>
     * The result is guaranteed to be consistent with the specification JSON schema.<br>
     * <br>
     * If implementations of the {@link SpecificationValidator} Service Provider Interface are provided, they will also
     * run against the {@link ImportSpecification} instance before the latter is returned.<br>
     * If the parsing, deserialization or validation (standard or via SPI implementations) fail, a
     * {@link SpecificationException} is thrown.
     * @return an {@link ImportSpecification}
     * @throws SpecificationException if parsing, deserialization or validation fail
     */
    public static ImportSpecification deserialize(Reader spec) throws SpecificationException {
        return deserialize(spec, Options.builder().build());
    }

    /**
     * Same as {@link ImportSpecificationDeserializer#deserialize(Reader)}, except it checks extra validation rules
     * against the provided {@link Neo4jDistribution} value.
     * @return an {@link ImportSpecification}
     * @throws SpecificationException if parsing, deserialization or validation fail
     * @deprecated use {@link #deserialize(Reader, Options)} with
     * {@link Options.Builder#neo4jDistribution(Neo4jDistribution)} instead
     */
    @Deprecated
    public static ImportSpecification deserialize(Reader spec, Neo4jDistribution neo4jDistribution)
            throws SpecificationException {

        return deserialize(
                spec, Options.builder().neo4jDistribution(neo4jDistribution).build());
    }

    /**
     * Returns a validated import specification using the supplied options.
     * Interpolation runs on the parsed JSON or YAML tree before schema validation.
     * @param rawSpecification the JSON or YAML specification
     * @param options deserialization options
     * @return an {@link ImportSpecification}
     * @throws SpecificationException if parsing, interpolation, deserialization or validation fail
     */
    public static ImportSpecification deserialize(Reader rawSpecification, Options options)
            throws SpecificationException {

        Objects.requireNonNull(options, "options");
        YAMLMapper mapper = initMapper();
        var variableInterpolator = new VariableInterpolator();

        JsonNode json = parse(mapper, rawSpecification);
        // TODO: integration tests
        json = variableInterpolator.interpolate(json, options);
        validateSchema(SCHEMA, json);

        ImportSpecification specification = deserialize(mapper, json);
        validateStatically(specification);
        validateRuntime(specification, options.getNeo4jDistribution());
        return specification;
    }

    /**
     * TODO
     */
    public static final class Options {
        // TODO: define sensible generic default paths
        private static final Set<String> DEFAULT_INTERPOLATION_PATHS = new LinkedHashSet<>();
        private final Neo4jDistribution neo4jDistribution;
        private final Map<String, Object> variables;
        private final Set<String> interpolationPaths;

        private Options(Builder builder) {
            neo4jDistribution = builder.neo4jDistribution;
            interpolationPaths = builder.interpolationEnabled && !builder.variables.isEmpty()
                    ? Collections.unmodifiableSet(
                            mergePaths(DEFAULT_INTERPOLATION_PATHS, builder.additionalInterpolationPaths))
                    : Set.of();
            variables = Collections.unmodifiableMap(builder.variables);
        }

        public static Builder builder() {
            return new Builder();
        }

        public Optional<Neo4jDistribution> getNeo4jDistribution() {
            return Optional.ofNullable(neo4jDistribution);
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public Set<String> getInterpolationPaths() {
            return interpolationPaths;
        }

        private static Set<String> mergePaths(Set<String> defaultPaths, Set<String> additionalPaths) {
            var paths = new LinkedHashSet<String>(defaultPaths.size() + additionalPaths.size());
            paths.addAll(defaultPaths);
            paths.addAll(additionalPaths);
            return paths;
        }

        public static final class Builder {
            // this allows all kinds of international letters and integers as well as underscore as separator
            static final Pattern EXACT_VARIABLE_NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L}\\p{Nd}_]*$");
            static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("\\p{L}[\\p{L}\\p{Nd}_]*");

            private Neo4jDistribution neo4jDistribution;
            private final Map<String, Object> variables = new LinkedHashMap<>();
            private boolean interpolationEnabled = false;
            private final Set<String> additionalInterpolationPaths = new LinkedHashSet<>();

            /**
             * Enables validation against the supplied Neo4j distribution.
             * @param distribution the Neo4j distribution
             * @return this builder
             */
            public Builder neo4jDistribution(Neo4jDistribution distribution) {
                neo4jDistribution = Objects.requireNonNull(distribution, "distribution");
                return this;
            }

            /**
             * TODO
             */
            public Builder interpolationVariables(Map<String, ?> values) {
                variables.putAll(values);
                return this;
            }

            /**
             * TODO
             */
            public Builder enableInterpolation() {
                interpolationEnabled = true;
                return this;
            }

            /**
             * TODO
             */
            public Builder disableInterpolation() {
                interpolationEnabled = false;
                return this;
            }

            /**
             * TODO
             */
            public Builder allowInterpolationAt(String path) {
                additionalInterpolationPaths.add(path);
                return this;
            }

            /**
             * TODO
             */
            public Options build() {
                if (!interpolationEnabled && !additionalInterpolationPaths.isEmpty()) {
                    throw new InvalidDeserializerOptionsException(
                            "Interpolation is disabled but additional interpolation paths are configured. "
                                    + "Enable interpolation or remove those paths");
                }
                if (variables.isEmpty() && !additionalInterpolationPaths.isEmpty()) {
                    throw new InvalidDeserializerOptionsException(
                            "Additional interpolation paths are configured but no interpolation variables are defined. "
                                    + "Define variables or remove these paths");
                }
                var errors = variables.entrySet().stream()
                        .flatMap((entry) -> variableErrorMessages(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
                if (!errors.isEmpty()) {
                    throw new InvalidDeserializerOptionsException(
                            String.format("Invalid variable definitions were found:\n%s", String.join("\n", errors)));
                }
                return new Options(this);
            }

            private static Stream<String> variableErrorMessages(String name, Object value) {
                var errors = new ArrayList<String>();
                nameErrorMessage(name).ifPresent(errors::add);
                valueErrorMessage(name, value).ifPresent(errors::add);
                return errors.stream();
            }

            private static Optional<String> nameErrorMessage(String name) {
                if (name == null) {
                    return Optional.of("Variable name cannot be null");
                }
                if (!EXACT_VARIABLE_NAME_PATTERN.matcher(name).matches()) {
                    return Optional.of(String.format(
                            "The name of variable '%s' must start with a letter, optionally followed by _ and/or other letters/numbers",
                            name));
                }
                return Optional.empty();
            }

            private static Optional<String> valueErrorMessage(String name, Object value) {
                if (value == null) {
                    return Optional.of(String.format("The value of variable '%s' cannot be null", name));
                }
                // TODO: this needs to be validated against actual usage in dataflow
                if (!(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
                    return Optional.of(String.format(
                            "The value of variable '%s' can only be a string, number or boolean, found: %s",
                            name, value.getClass()));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Validates the consistency of the provided {@link ImportSpecification} instance.
     * <br>
     * The validation is performed by the registered implementations of the {@link SpecificationValidator} SPI.
     * This method does not check whether the provided {@link ImportSpecification} instance complies to the constraints defined
     * in the JSON schema, but assumes it does.
     * <br>
     * This method is deprecated as {@link ImportSpecificationDeserializer#deserialize(Reader)} is the only recommended
     * way to retrieve a fully valid {@link ImportSpecification} instance.
     *
     * @param specification the import specification to run validations against
     * @throws SpecificationException if validation fails
     */
    @Deprecated
    public static void validateStatically(ImportSpecification specification) throws SpecificationException {
        SpecificationValidators.of(loadValidators()).validate(specification);
    }

    private static YAMLMapper initMapper() {
        var module = new SimpleModule();
        module.addDeserializer(Source.class, new SourceDeserializer(loadProviders(SourceProvider.class)));
        module.addDeserializer(Action.class, new ActionDeserializer(loadProviders(ActionProvider.class)));
        return YAMLMapper.builder()
                .addModule(module)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .build();
    }

    private static JsonNode parse(ObjectMapper mapper, Reader spec) throws SpecificationException {
        try {
            return mapper.readTree(spec);
        } catch (IOException e) {
            throw new UnparseableSpecificationException(e);
        }
    }

    private static ImportSpecification deserialize(ObjectMapper mapper, JsonNode json) throws SpecificationException {
        try {
            return mapper.treeToValue(json, ImportSpecification.class);
        } catch (JsonProcessingException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SourceError) {
                throw new UndeserializableSourceException(cause);
            }
            if (cause instanceof ActionError) {
                throw new UndeserializableActionException(cause);
            }
            throw new UndeserializableSpecificationException(
                    "The payload cannot be deserialized, despite a successful schema validation.\n"
                            + "This is likely a bug, please open an issue in "
                            + "https://github.com/neo4j/import-spec/issues/new and share the specification that caused the issue",
                    e);
        }
    }

    private static void validateSchema(JsonSchema schema, JsonNode json) throws InvalidSpecificationException {
        Builder builder = SpecificationValidationResult.builder();
        schema.validate(json)
                .forEach(msg -> builder.addError(
                        msg.getInstanceLocation().toString(),
                        String.format("SCHM-%s", msg.getCode()),
                        msg.getMessage()));
        SpecificationValidationResult result = builder.build();
        if (!result.passes()) {
            throw new InvalidSpecificationException(result);
        }
    }

    private static List<SpecificationValidator> loadValidators() {
        return ServiceLoader.load(SpecificationValidator.class).stream()
                .map(Provider::get)
                .collect(Collectors.toList());
    }

    private static void validateRuntime(ImportSpecification spec, Optional<Neo4jDistribution> neo4jDistribution)
            throws SpecificationException {
        if (neo4jDistribution
                .filter(distribution -> distribution.isVersionLargerThanOrEqual("4.4"))
                .isEmpty()) {
            return;
        }
        var runtimeValidator = new Neo4jDistributionValidator(neo4jDistribution.get());
        SpecificationValidators.of(runtimeValidator).validate(spec);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> loadProviders(Class<?> type) {
        return ServiceLoader.load(type).stream()
                .map(provider -> (T) provider.get())
                .collect(Collectors.toList());
    }
}
