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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.graph.Graph;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.SpecificationException;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;
import org.neo4j.importer.v1.validation.UndeserializableSpecificationException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

public class ImportSpecificationDeserializer {
    private static final YAMLMapper MAPPER = YAMLMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .disable(MapperFeature.AUTO_DETECT_CREATORS)
            .build();

    private static final JsonSchema SCHEMA = JsonSchemaFactory.getInstance(VersionFlag.V202012)
            .getSchema(ImportSpecificationDeserializer.class.getResourceAsStream("/spec.v1.json"));

    /**
     * Returns an instance of {@link ImportSpecification} based on the provided {@link Reader} content.
     * The result is guaranteed to be consistent with the specification JSON schema.
     * <br>
     * If implementations of the {@link SpecificationValidator} SPI are provided, they will also run against the
     * {@link ImportSpecification} instance before the latter is returned.
     * <br>
     * If the parsing, deserialization or validation (standard or via SPI implementations) fail, a {@link SpecificationException}
     * is going to be thrown.
     *
     * @return an {@link ImportSpecification}
     * @throws SpecificationException if parsing, deserialization or validation fail
     */
    public static ImportSpecification deserialize(Reader spec) throws SpecificationException {
        JsonNode json = parse(spec);
        validate(SCHEMA, json);
        ImportSpecification result = deserialize(json);
        runExtraValidations(result);
        return result;
    }

    private static void validate(JsonSchema schema, JsonNode json) throws InvalidSpecificationException {
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

    private static ImportSpecification deserialize(JsonNode json) throws SpecificationException {
        try {
            return MAPPER.treeToValue(json, ImportSpecification.class);
        } catch (JsonProcessingException e) {
            throw new UndeserializableSpecificationException(
                    "The payload cannot be deserialized, despite a successful schema validation.\n"
                            + "This is likely a bug, please open an issue in "
                            + "https://github.com/neo4j/import-spec/issues/new and share the specification that caused the issue",
                    e);
        }
    }

    private static void runExtraValidations(ImportSpecification spec) throws SpecificationException {
        var validators = loadValidators();
        validators.forEach(validator -> validator.visitConfiguration(spec.getConfiguration()));
        var sources = spec.getSources();
        for (int i = 0; i < sources.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitSource(index, sources.get(index)));
        }
        var targets = spec.getTargets();
        var nodeTargets = targets.getNodes();
        for (int i = 0; i < nodeTargets.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitNodeTarget(index, nodeTargets.get(index)));
        }
        var relationshipTargets = targets.getRelationships();
        for (int i = 0; i < relationshipTargets.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitRelationshipTarget(index, relationshipTargets.get(index)));
        }
        var queryTargets = targets.getCustomQueries();
        for (int i = 0; i < queryTargets.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitCustomQueryTarget(index, queryTargets.get(index)));
        }
        var actions = spec.getActions() == null ? Collections.<Action>emptyList() : spec.getActions();
        for (int i = 0; i < actions.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitAction(index, actions.get(index)));
        }

        Set<Class<? extends SpecificationValidator>> failedValidations = new HashSet<>(validators.size());
        var builder = SpecificationValidationResult.builder();
        validators.forEach(validator -> {
            for (Class<? extends SpecificationValidator> dependent : validator.requires()) {
                if (failedValidations.contains(dependent)) {
                    return;
                }
            }
            if (validator.report(builder)) {
                failedValidations.add(validator.getClass());
            }
        });
        SpecificationValidationResult result = builder.build();
        if (!result.passes()) {
            throw new InvalidSpecificationException(result);
        }
    }

    private static List<SpecificationValidator> loadValidators() {
        var validatorCatalog = new HashMap<Class<? extends SpecificationValidator>, SpecificationValidator>();
        var validatorGraph =
                new HashMap<Class<? extends SpecificationValidator>, Set<Class<? extends SpecificationValidator>>>();
        ServiceLoader.load(SpecificationValidator.class).forEach(validator -> {
            validatorCatalog.put(validator.getClass(), validator);
            validatorGraph.put(validator.getClass(), validator.requires());
        });
        return Graph.runTopologicalSort(validatorGraph).stream()
                .map(validatorCatalog::get)
                .collect(Collectors.toList());
    }

    private static JsonNode parse(Reader spec) throws SpecificationException {
        try {
            return MAPPER.readTree(spec);
        } catch (IOException e) {
            throw new UnparseableSpecificationException(e);
        }
    }
}
