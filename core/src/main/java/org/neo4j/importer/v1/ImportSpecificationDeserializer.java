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
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionDeserializer;
import org.neo4j.importer.v1.actions.ActionProvider;
import org.neo4j.importer.v1.distribution.Neo4jDistribution;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceDeserializer;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.validation.ActionError;
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
        return deserialize(spec, Optional.empty());
    }

    /**
     * Same as {@link ImportSpecificationDeserializer#deserialize(Reader)}, except it checks extra validation rules
     * against the provided {@link Neo4jDistribution} value.
     * @return an {@link ImportSpecification}
     * @throws SpecificationException if parsing, deserialization or validation fail
     */
    public static ImportSpecification deserialize(Reader spec, Neo4jDistribution neo4jDistribution)
            throws SpecificationException {

        return deserialize(spec, Optional.of(neo4jDistribution));
    }

    private static ImportSpecification deserialize(
            Reader rawSpecification, Optional<Neo4jDistribution> neo4jDistribution) throws SpecificationException {

        YAMLMapper mapper = initMapper();
        JsonNode json = parse(mapper, rawSpecification);
        validateSchema(SCHEMA, json);

        ImportSpecification specification = deserialize(mapper, json);
        validateStatically(specification);
        validateRuntime(specification, neo4jDistribution);
        return specification;
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
