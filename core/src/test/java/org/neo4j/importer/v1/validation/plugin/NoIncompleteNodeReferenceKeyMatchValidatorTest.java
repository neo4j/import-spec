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
package org.neo4j.importer.v1.validation.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeMatchMode;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeUniqueConstraint;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Targets;
import org.neo4j.importer.v1.targets.WriteMode;
import org.neo4j.importer.v1.validation.SpecificationError;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;
import org.neo4j.importer.v1.validation.plugin.NoIncompleteNodeReferenceKeyMatchValidator.LookupType;

class NoIncompleteNodeReferenceKeyMatchValidatorTest {

    private static final String SOURCE_NAME = "a-source";

    private static final String NODE_LABEL = "ALabel";

    private static final Random RAND = new Random();

    private final SpecificationValidator validator = new NoIncompleteNodeReferenceKeyMatchValidator();

    @ParameterizedTest
    @MethodSource("matching_property_combinations")
    void passes_if_node_reference_keys_match_unique_or_key_properties(List<String> nodeReferenceKeys) {
        var spec = importSpec(
                node("a-node", keyProperties(List.of("a", "c"), List.of("b")), uniqueProperties(List.of("d", "e"))),
                relationship(startNodeReference("a-node", nodeReferenceKeys)));
        validator.visitNodeTarget(0, spec.getTargets().getNodes().get(0));
        validator.visitRelationshipTarget(
                0, spec.getTargets().getRelationships().get(0));
        var reportBuilder = new Builder();

        boolean result = validator.report(reportBuilder);

        assertThat(result)
                .overridingErrorMessage(String.format("expected no error message, but got %s", reportBuilder.build()))
                .isFalse();
    }

    @ParameterizedTest
    @MethodSource("incomplete_property_combinations")
    void fails_if_node_reference_keys_only_partially_match_unique_or_key_properties(
            List<String> nodeReferenceKeys, Map<LookupProperty, List<String>> allMissingProperties) {
        var spec = importSpec(
                node("a-node", keyProperties(List.of("a", "c"), List.of("b")), uniqueProperties(List.of("d", "e"))),
                relationship(startNodeReference("a-node", nodeReferenceKeys)));
        validator.visitNodeTarget(0, spec.getTargets().getNodes().get(0));
        validator.visitRelationshipTarget(
                0, spec.getTargets().getRelationships().get(0));
        var reportBuilder = new Builder();

        boolean result = validator.report(reportBuilder);

        assertThat(result)
                .overridingErrorMessage("expected error messages, but got none")
                .isTrue();
        var errorMessages = reportBuilder.build().getErrors().stream()
                .map(SpecificationError::getMessage)
                .collect(Collectors.toList());
        assertThat(errorMessages).hasSize(allMissingProperties.size());
        allMissingProperties.forEach((mappedKey, missingProperties) -> {
            assertThat(errorMessages).anyMatch(message -> {
                String expectedErrorMessage = String.format(
                        "Insufficient key mapping for node reference 'a-node'. Please also map ['%s'] alongside '%s' to fully match the node target's %s constraint",
                        String.join("', '", missingProperties),
                        mappedKey.name,
                        mappedKey.type.toString().toLowerCase(Locale.ROOT));
                return message.startsWith(expectedErrorMessage);
            });
        });
    }

    @Test
    void displays_closest_match_with_largest_number_of_properties_when_validation_fails() {
        var spec = importSpec(
                node(
                        "a-node",
                        keyProperties(List.of("a", "c"), List.of("b")),
                        uniqueProperties(List.of("a", "f", "g"))),
                relationship(startNodeReference("a-node", List.of("a"))));
        validator.visitNodeTarget(0, spec.getTargets().getNodes().get(0));
        validator.visitRelationshipTarget(
                0, spec.getTargets().getRelationships().get(0));
        var reportBuilder = new Builder();

        boolean result = validator.report(reportBuilder);
        assertThat(result)
                .overridingErrorMessage("expected an error message, but got none")
                .isTrue();
        var errorMessages = reportBuilder.build().getErrors().stream()
                .map(SpecificationError::getMessage)
                .collect(Collectors.toList());
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0))
                .startsWith(
                        "Insufficient key mapping for node reference 'a-node'. Please also map ['f', 'g'] alongside 'a' to fully match the node target's unique constraint 'unique-constraint-");
    }

    private static Stream<Arguments> matching_property_combinations() {
        return Stream.of(
                Arguments.of(List.of("a", "b", "c")),
                Arguments.of(List.of("a", "b", "c", "d", "e")),
                Arguments.of(List.of("a", "c")),
                Arguments.of(List.of("a", "c", "d", "e")),
                Arguments.of(List.of("b")),
                Arguments.of(List.of("b", "d", "e")),
                Arguments.of(List.of("d", "e")));
    }

    private static Stream<Arguments> incomplete_property_combinations() {
        return Stream.of(
                Arguments.of(List.of("a"), Map.of(LookupProperty.key("a"), List.of("c"))),
                Arguments.of(
                        List.of("a", "d"),
                        Map.of(LookupProperty.key("a"), List.of("c"), LookupProperty.unique("d"), List.of("e"))),
                Arguments.of(
                        List.of("a", "e"),
                        Map.of(LookupProperty.key("a"), List.of("c"), LookupProperty.unique("e"), List.of("d"))),
                Arguments.of(List.of("c"), Map.of(LookupProperty.key("c"), List.of("a"))),
                Arguments.of(
                        List.of("c", "d"),
                        Map.of(LookupProperty.key("c"), List.of("a"), LookupProperty.unique("d"), List.of("e"))),
                Arguments.of(
                        List.of("c", "e"),
                        Map.of(LookupProperty.key("c"), List.of("a"), LookupProperty.unique("e"), List.of("d"))));
    }

    private static ImportSpecification importSpec(NodeTarget node, RelationshipTarget relationship) {
        return new ImportSpecification(
                "1",
                null,
                List.of(new DummySource(SOURCE_NAME)),
                new Targets(List.of(node), List.of(relationship), null),
                null);
    }

    private static NodeTarget node(
            String name, List<NodeKeyConstraint> keys, List<NodeUniqueConstraint> uniques, String... extraProps) {
        var properties = extractProperties(keys, uniques);
        properties.addAll(List.of(extraProps));
        return new NodeTarget(
                true,
                name,
                SOURCE_NAME,
                null,
                WriteMode.CREATE,
                (ObjectNode) null,
                List.of(NODE_LABEL),
                imagineMappingsFor(properties),
                new NodeSchema(null, keys, uniques, null, null, null, null, null, null));
    }

    private static RelationshipTarget relationship(NodeReference startNodeReference) {
        return new RelationshipTarget(
                true,
                "a-relationship-target",
                SOURCE_NAME,
                null,
                "SELF_LINKS_TO",
                WriteMode.CREATE,
                NodeMatchMode.MATCH,
                (ObjectNode) null,
                startNodeReference,
                new NodeReference(startNodeReference.getName()),
                null,
                null);
    }

    @SafeVarargs
    private List<NodeKeyConstraint> keyProperties(List<String>... keyProps) {
        return Arrays.stream(keyProps)
                .map(props -> new NodeKeyConstraint(
                        String.format("key-constraint-%d", Math.abs(RAND.nextInt()) % 2048), NODE_LABEL, props, null))
                .collect(Collectors.toList());
    }

    @SafeVarargs
    private List<NodeUniqueConstraint> uniqueProperties(List<String>... uniqueProps) {
        return Arrays.stream(uniqueProps)
                .map(props -> new NodeUniqueConstraint(
                        String.format("unique-constraint-%d", Math.abs(RAND.nextInt()) % 2048),
                        NODE_LABEL,
                        props,
                        null))
                .collect(Collectors.toList());
    }

    private NodeReference startNodeReference(String name, List<String> keys) {
        return new NodeReference(
                name,
                IntStream.range(0, keys.size())
                        .mapToObj(i -> new KeyMapping(String.format("source-field-%d", i), keys.get(i)))
                        .collect(Collectors.toList()));
    }

    private static List<String> extractProperties(List<NodeKeyConstraint> keys, List<NodeUniqueConstraint> uniques) {
        var result = new LinkedHashSet<String>();
        keys.stream()
                .flatMap((NodeKeyConstraint constraint) -> constraint.getProperties().stream())
                .forEach(result::add);
        uniques.stream()
                .flatMap((NodeUniqueConstraint constraint) -> constraint.getProperties().stream())
                .forEach(result::add);
        return new ArrayList<>(result);
    }

    private static List<PropertyMapping> imagineMappingsFor(List<String> properties) {
        return IntStream.range(0, properties.size())
                .mapToObj(i -> new PropertyMapping(String.format("source-field-%d", i), properties.get(i), null))
                .collect(Collectors.toList());
    }

    private static class DummySource implements Source {

        private final String name;

        public DummySource(String name) {
            this.name = name;
        }

        @Override
        public String getType() {
            return "dummy";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DummySource)) return false;
            DummySource that = (DummySource) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    private static class LookupProperty {

        private final String name;
        private final LookupType type;

        private LookupProperty(String name, LookupType type) {
            this.name = name;
            this.type = type;
        }

        public static LookupProperty key(String name) {
            return new LookupProperty(name, LookupType.KEY);
        }

        public static LookupProperty unique(String name) {
            return new LookupProperty(name, LookupType.UNIQUE);
        }

        public String getName() {
            return name;
        }

        public LookupType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LookupProperty)) return false;
            LookupProperty that = (LookupProperty) o;
            return Objects.equals(name, that.name) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }
}
