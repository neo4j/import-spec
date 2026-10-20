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
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
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
import org.neo4j.importer.v1.validation.interpolation.InterpolationVariableValidator;
import org.neo4j.importer.v1.validation.interpolation.InterpolationVariableValidators;

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
        json = variableInterpolator.interpolate(json, options);
        validateSchema(SCHEMA, json);

        ImportSpecification specification = deserialize(mapper, json);
        validateStatically(specification);
        validateRuntime(specification, options.getNeo4jDistribution());
        return specification;
    }

    /**
     * {@code Options} configures Neo4j distribution validation and variable interpolation.
     * Both are disabled by default.
     */
    public static final class Options {
        private static final Set<String> DEFAULT_INTERPOLATION_PATHS = Set.of(
                "$.targets.nodes[*].active",
                "$.targets.nodes[*].labels[*]",
                "$.targets.nodes[*].schema.type_constraints[*].label",
                "$.targets.nodes[*].schema.key_constraints[*].label",
                "$.targets.nodes[*].schema.unique_constraints[*].label",
                "$.targets.nodes[*].schema.existence_constraints[*].label",
                "$.targets.nodes[*].schema.range_indexes[*].label",
                "$.targets.nodes[*].schema.text_indexes[*].label",
                "$.targets.nodes[*].schema.point_indexes[*].label",
                "$.targets.nodes[*].schema.fulltext_indexes[*].labels[*]",
                "$.targets.nodes[*].schema.vector_indexes[*].label",
                "$.targets.relationships[*].active",
                "$.targets.relationships[*].type",
                "$.targets.queries[*].active",
                "$.targets.queries[*].query",
                "$.actions[*].active",
                "$.actions[?(@.type =~ /^cypher$/i)].query");
        private final Neo4jDistribution neo4jDistribution;
        private final Map<String, Object> variables;
        private final Set<String> interpolationPaths;

        private Options(Builder builder) {
            neo4jDistribution = builder.neo4jDistribution;
            interpolationPaths = builder.interpolationEnabled && !builder.variables.isEmpty()
                    ? Set.copyOf(mergePaths(DEFAULT_INTERPOLATION_PATHS, builder.additionalInterpolationPaths))
                    : Set.of();
            variables = Map.copyOf(builder.variables);
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

        /**
         * {@code getInterpolationPaths} returns the allowed json paths, or an empty set if interpolation is effectively
         * disabled.
         * @return the allowed interpolation paths
         */
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
            private Neo4jDistribution neo4jDistribution;
            private final Map<String, Object> variables = new LinkedHashMap<>();
            private boolean interpolationEnabled = false;
            private final Set<String> additionalInterpolationPaths = new LinkedHashSet<>();

            /**
             * {@code neo4jDistribution} enables validation against the supplied Neo4j distribution.
             * @param distribution the Neo4j distribution
             * @return this builder
             */
            public Builder neo4jDistribution(Neo4jDistribution distribution) {
                neo4jDistribution = Objects.requireNonNull(distribution, "distribution");
                return this;
            }

            /**
             * {@code interpolationVariables} adds variables, replacing existing values with the same name.
             * Names must start with a letter and contain only letters, digits or underscores.
             * Values must be non-null strings, numbers or booleans.
             * @param values the variables, referenced as {@code $name} in the specification
             * @return this builder
             */
            public Builder interpolationVariables(Map<String, ?> values) {
                variables.putAll(values);
                return this;
            }

            public Builder enableInterpolation() {
                interpolationEnabled = true;
                return this;
            }

            public Builder disableInterpolation() {
                interpolationEnabled = false;
                return this;
            }

            /**
             * @see #allowInterpolationsAt(List)
             */
            public Builder allowInterpolationAt(String path) {
                return allowInterpolationsAt(List.of(path));
            }

            /**
             * @see #allowInterpolationsAt(List)
             */
            public Builder allowInterpolationsAt(String path, String... paths) {
                var allPaths = new ArrayList<String>(1 + paths.length);
                allPaths.add(path);
                allPaths.addAll(List.of(paths));
                return allowInterpolationsAt(allPaths);
            }

            /**
             * {@code allowInterpolationsAt} adds json paths to the default interpolation paths.
             * Default paths cover target and action {@code active} fields, node {@code labels} and relationship {@code type},
             * node constraint and index labels, and query target and Cypher action {@code query} fields.
             * @param paths additional JSON paths where interpolation is allowed
             * @return this builder
             */
            public Builder allowInterpolationsAt(List<String> paths) {
                additionalInterpolationPaths.addAll(paths);
                return this;
            }

            /**
             * {@code build} validates the configuration and creates the options.
             * @return the deserialization options
             * @throws InvalidDeserializerOptionsException if variables are invalid, or additional paths are configured
             * while interpolation is disabled or no variables are defined
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

                var pathErrors = additionalInterpolationPaths.stream()
                        .flatMap(Builder::pathErrorMessage)
                        .collect(Collectors.toList());
                if (!pathErrors.isEmpty()) {
                    throw new InvalidDeserializerOptionsException(String.format(
                            "Interpolation path(s) parsing errors were found:\n%s", String.join("\n", pathErrors)));
                }

                InterpolationVariableValidators.of(loadInterpolationVariableValidators())
                        .validate(variables);

                return new Options(this);
            }

            private static Stream<String> pathErrorMessage(String path) {
                try {
                    compileStrict(path);
                    return Stream.empty();
                } catch (InvalidPathException invalidPathException) {
                    return Stream.of(
                            String.format("Path %s did not compile: %s", path, invalidPathException.getMessage()));
                }
            }

            private static JsonPath compileStrict(String path) {
                if (path == null) {
                    throw new InvalidPathException("Path must not be null");
                }
                if (!path.startsWith("$.")) {
                    throw new InvalidPathException("Path must start with '$.'");
                }
                return JsonPath.compile(path);
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
        SpecificationValidators.of(loadSpecificationValidators()).validate(specification);
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

    private static List<SpecificationValidator> loadSpecificationValidators() {
        return ServiceLoader.load(SpecificationValidator.class).stream()
                .map(Provider::get)
                .collect(Collectors.toList());
    }

    private static List<InterpolationVariableValidator> loadInterpolationVariableValidators() {
        return ServiceLoader.load(InterpolationVariableValidator.class).stream()
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
