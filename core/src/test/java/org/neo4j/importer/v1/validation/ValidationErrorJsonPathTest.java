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
package org.neo4j.importer.v1.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionDeserializer;
import org.neo4j.importer.v1.actions.ActionProvider;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceDeserializer;
import org.neo4j.importer.v1.sources.SourceProvider;

class ValidationErrorJsonPathTest {

    private static final YAMLMapper MAPPER = mapper();

    @TestFactory
    @DisplayName("error paths")
    Stream<DynamicTest> validator_error_paths() {
        return ServiceLoader.load(SpecificationValidator.class).stream().map(ValidationErrorJsonPathTest::test);
    }

    private static DynamicTest test(ServiceLoader.Provider<SpecificationValidator> provider) {
        return DynamicTest.dynamicTest(
                String.format("from %s are valid", provider.type().getSimpleName()),
                () -> assertErrorPathsAreValid(provider.get()));
    }

    private static void assertErrorPathsAreValid(SpecificationValidator validator) throws IOException {
        var fixtureName = validator.getClass().getSimpleName() + ".json";
        var root = loadJson(String.format("e2e/error-paths/%s", fixtureName));
        var specification = MAPPER.treeToValue(root, ImportSpecification.class);
        var result = validate(validator, specification);
        var errors = result.getErrors();

        assertThat(errors)
                .as("%s should report at least one error", validator.getClass().getName())
                .isNotEmpty();
        errors.forEach(error -> {
            var code = error.getCode();
            assertThat(code)
                    .as("%s should report an error code", validator.getClass().getName())
                    .isNotBlank();
            assertThat(pathExists(root, error.getElementPath()))
                    .as("[%s] %s should resolve in %s", code, error.getElementPath(), fixtureName)
                    .isTrue();
        });
    }

    private static SpecificationValidationResult validate(SpecificationValidator validator, ImportSpecification spec) {
        validator.visitConfiguration(spec.getConfiguration());
        var sources = spec.getSources();
        for (int i = 0; i < sources.size(); i++) {
            validator.visitSource(i, sources.get(i));
        }

        var targets = spec.getTargets();
        var nodeTargets = targets.getNodes();
        for (int i = 0; i < nodeTargets.size(); i++) {
            validator.visitNodeTarget(i, nodeTargets.get(i));
        }

        var relationshipTargets = targets.getRelationships();
        for (int i = 0; i < relationshipTargets.size(); i++) {
            validator.visitRelationshipTarget(i, relationshipTargets.get(i));
        }

        var queryTargets = targets.getCustomQueries();
        for (int i = 0; i < queryTargets.size(); i++) {
            validator.visitCustomQueryTarget(i, queryTargets.get(i));
        }

        var actions = spec.getActions();
        for (int i = 0; i < actions.size(); i++) {
            validator.visitAction(i, actions.get(i));
        }

        var builder = SpecificationValidationResult.builder();
        validator.report(builder);
        return builder.build();
    }

    private static boolean pathExists(JsonNode root, String path) {
        assertThat(path).as("error path").startsWith("$");

        var current = root;
        var offset = 1;
        while (offset < path.length()) {
            var next = path.charAt(offset);
            switch (next) {
                case '.':
                    var start = ++offset;
                    while (offset < path.length() && path.charAt(offset) != '.' && path.charAt(offset) != '[') {
                        offset++;
                    }
                    current = current.get(path.substring(start, offset));
                    break;
                case '[':
                    var end = path.indexOf(']', offset);
                    assertThat(end).as("%s should close array index", path).isGreaterThanOrEqualTo(0);
                    assertThat(current.isArray())
                            .as("%s should resolve array at offset %d", path, offset)
                            .isTrue();
                    current = current.get(Integer.parseInt(path.substring(offset + 1, end)));
                    offset = end + 1;
                    break;
                default:
                    return false;
            }

            if (current == null || current.isMissingNode()) {
                return false;
            }
        }
        return true;
    }

    private static JsonNode loadJson(String resourceName) throws IOException {
        try (InputStream stream =
                ValidationErrorJsonPathTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(stream).as("resource %s", resourceName).isNotNull();
            return MAPPER.readTree(stream);
        }
    }

    private static YAMLMapper mapper() {
        var module = new SimpleModule();
        module.addDeserializer(Source.class, new SourceDeserializer(loadSourceProviders()));
        module.addDeserializer(Action.class, new ActionDeserializer(loadActionProviders()));
        return YAMLMapper.builder()
                .addModule(module)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .build();
    }

    private static List<SourceProvider<? extends Source>> loadSourceProviders() {
        return ServiceLoader.load(SourceProvider.class).stream()
                .map(provider -> (SourceProvider<? extends Source>) provider.get())
                .collect(Collectors.toList());
    }

    private static List<ActionProvider<? extends Action>> loadActionProviders() {
        return ServiceLoader.load(ActionProvider.class).stream()
                .map(provider -> (ActionProvider<? extends Action>) provider.get())
                .collect(Collectors.toList());
    }
}
