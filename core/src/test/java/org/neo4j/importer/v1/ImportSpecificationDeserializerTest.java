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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.importer.v1.actions.ActionStage;
import org.neo4j.importer.v1.actions.plugin.CypherAction;
import org.neo4j.importer.v1.actions.plugin.CypherExecutionMode;
import org.neo4j.importer.v1.distribution.Neo4jDistributions;
import org.neo4j.importer.v1.sources.BigQuerySource;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.Targets;
import org.neo4j.importer.v1.targets.WriteMode;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.UndeserializableActionException;
import org.neo4j.importer.v1.validation.UndeserializableSourceException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

class ImportSpecificationDeserializerTest {

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_minimal_job_spec(SpecFormat format, TestInfo testInfo) throws Exception {

        try (var reader = specReader(format, testInfo)) {
            var spec = deserialize(reader);

            assertThat(spec.getConfiguration()).isEqualTo(new Configuration(null));
            assertThat(spec.getSources())
                    .isEqualTo(List.of(new BigQuerySource("my-bigquery-source", "SELECT id, name FROM my.table")));
            assertThat(spec.getTargets())
                    .isEqualTo(new Targets(
                            null,
                            null,
                            List.of(new CustomQueryTarget(
                                    true,
                                    "my-query",
                                    "my-bigquery-source",
                                    null,
                                    "UNWIND $rows AS row CREATE (n:ANode) SET n = row"))));
            assertThat(spec.getActions()).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_job_spec(SpecFormat format, TestInfo testInfo) throws Exception {

        try (var reader = specReader(format, testInfo)) {
            var spec = deserialize(reader);

            assertThat(spec.getConfiguration())
                    .isEqualTo(new Configuration(Map.of("foo", "bar", "baz", 42, "qix", List.of(true, 1.0, Map.of()))));
            assertThat(spec.getSources())
                    .isEqualTo(List.of(new BigQuerySource("my-bigquery-source", "SELECT id, name FROM my.table")));
            assertThat(spec.getTargets())
                    .isEqualTo(new Targets(
                            null,
                            null,
                            List.of(new CustomQueryTarget(
                                    true,
                                    "my-query",
                                    "my-bigquery-source",
                                    null,
                                    "UNWIND $rows AS row CREATE (n:ANode) SET n = row"))));
            assertThat(spec.getActions())
                    .isEqualTo(List.of(new CypherAction(
                            true,
                            "my-cypher-action",
                            ActionStage.START,
                            "CREATE (:Started)",
                            CypherExecutionMode.TRANSACTION)));
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_job_spec_with_uppercase_write_mode_and_property_type(SpecFormat format, TestInfo testInfo)
            throws Exception {

        try (var reader = specReader(format, testInfo)) {
            var spec = deserialize(reader);

            assertThat(spec.getConfiguration()).isEqualTo(new Configuration(null));
            assertThat(spec.getSources())
                    .isEqualTo(List.of(new BigQuerySource("my-bigquery-source", "SELECT id, name FROM my.table")));
            assertThat(spec.getTargets())
                    .isEqualTo(new Targets(
                            List.of(new NodeTarget(
                                    true,
                                    "my-node",
                                    "my-bigquery-source",
                                    null,
                                    WriteMode.CREATE,
                                    (ObjectNode) null,
                                    List.of("ALabel"),
                                    List.of(new PropertyMapping("id", "id", PropertyType.STRING)),
                                    null)),
                            null,
                            null));
            assertThat(spec.getActions()).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_spec_is_unparseable(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(UnparseableSpecificationException.class);
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_spec_is_not_deserializable(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: array found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_version_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'version' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_version_is_invalid(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.version: must be the constant value '1'");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_sources_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'sources' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_sources_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_sources_are_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_targets_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'targets' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_targets_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.targets: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_targets_are_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "3 error(s)",
                        "0 warning(s)",
                        "$.targets: required property 'nodes' not found",
                        "$.targets: required property 'relationships' not found",
                        "$.targets: required property 'queries' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_targets_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_targets_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_custom_query_targets_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.queries: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_actions_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_missing_in_source(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_missing_in_node_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_missing_in_relationship_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_missing_in_custom_query_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.queries[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_missing_in_action(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_blank_in_source(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_blank_in_node_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_blank_in_relationship_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_blank_in_custom_query_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_name_is_blank_in_action(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_in_array_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_active_attribute_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].active: integer found, boolean expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_is_missing_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0]: required property 'type' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_type_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0].type: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_type_is_unsupported(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining("Expected exactly one action provider for action of type foobar, but found: 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_stage_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0].stage: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_action_stage_is_unsupported(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].stage: does not have a value in the enumeration");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_cypher_action_execution_mode_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining(
                        "Action provider org.neo4j.importer.v1.actions.plugin.CypherActionProvider failed to deserialize");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_cypher_action_execution_mode_is_unsupported(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining(
                        "Action provider org.neo4j.importer.v1.actions.plugin.CypherActionProvider failed to deserialize");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_name_is_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.sources[1].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_name_is_duplicated_with_target_name(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.targets.nodes[0].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_name_is_duplicated_with_action_name(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.actions[0].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_name_is_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.nodes[1].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_name_is_duplicated_with_rel_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.relationships[0].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_name_is_duplicated_with_custom_query_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.queries[0].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_query_target_name_is_duplicated_with_action(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.queries[0].name, $.actions[0].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_does_not_refer_to_existing_source(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_does_not_refer_to_existing_source(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_custom_query_target_does_not_refer_to_existing_source(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_depends_on_non_existing_target(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] depends on a non-existing target \"invalid\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_depends_on_non_existing_target_or_action(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0] depends on a non-existing target \"invalid\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_custom_query_target_depends_on_non_existing_target_or_action(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0] depends on a non-existing target \"invalid\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_refers_to_a_non_existing_node_target_for_start(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_active_relationship_target_refers_to_an_inactive_node_target_for_start(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference belongs to an active target but refers to an inactive node target \"a-node-target\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_active_relationship_target_refers_to_an_inactive_node_target_for_end(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference belongs to an active target but refers to an inactive node target \"another-node-target\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_inactive_relationship_refers_to_an_inactive_node_target_for_start(
            SpecFormat format, TestInfo testInfo) {

        assertThatNoException().isThrownBy(() -> {
            try (var reader = specReader(format, testInfo)) {
                deserialize(reader);
            }
        });
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_inactive_relationship_refers_to_an_inactive_node_target_for_end(
            SpecFormat format, TestInfo testInfo) {

        assertThatNoException().isThrownBy(() -> {
            try (var reader = specReader(format, testInfo)) {
                deserialize(reader);
            }
        });
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_inactive_node_reference_when_other_references_are_dangling(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_refers_to_a_non_existing_node_target_for_end(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_direct_dependency_cycle_is_detected(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "A dependency cycle has been detected: a-target->a-target");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_longer_dependency_cycle_is_detected(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-relationship-target->a-query-target->another-query-target->a-relationship-target");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_dependency_cycle_is_detected_via_start_node_reference(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-node-target->a-relationship-target->a-node-target");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_dependency_cycle_is_detected_via_end_node_reference(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-node-target->a-relationship-target->a-node-target");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_cycles_if_names_are_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"a-target\" is duplicated across the following paths: $.targets.relationships[0].name, $.targets.queries[0].name");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_cycles_if_depends_on_are_dangling(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[1] depends on a non-existing target \"invalid\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_cycles_if_node_references_are_dangling(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[1].start_node_reference refers to a non-existing node target \"invalid-ref\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_all_targets_are_inactive(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "[$.targets] at least one target must be active, none found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_duplicates_dependency(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].depends_on defines dependency \"a-query-target\" 2 times, it must be defined at most once");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_duplicates_dependency(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].depends_on defines dependency \"a-query-target\" 2 times, it must be defined at most once");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_query_target_duplicates_dependency(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_cycles_if_targets_duplicate_dependency(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_are_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels[0] \"Label1\" must be defined only once but found 2 occurrences",
                        "$.targets.nodes[0].labels[2] \"Label2\" must be defined only once but found 3 occurrences");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_is_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_is_duplicated(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_type_constraint_refers_to_non_existent_property(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_type_constraint_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_type_constraint_property_refers_to_an_untyped_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property \"untyped\" refers to an untyped property");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_type_constraint_property_refers_to_an_untyped_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property \"untyped\" refers to an untyped property");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_unique_constraint_refers_to_non_existent_property(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_unique_constraint_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_key_constraint_refers_to_non_existent_property(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_key_constraint_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_existence_constraint_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_existence_constraint_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_range_index_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_range_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_text_index_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_text_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_point_index_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_point_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_fulltext_index_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0] \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_fulltext_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_vector_index_refers_to_non_existent_label(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_vector_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_type_constraint_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_key_constraint_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_unique_constraint_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_existence_constraint_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_range_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_text_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_point_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_fulltext_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_vector_index_property_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property refers to a non-existing node property \"not-a-valid-property\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_refers_to_non_existent_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property refers to a non-existing node property \"not-a-valid-property\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_refers_to_non_key_and_non_unique_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.relationships[0].start_node_reference.key_mappings[0].node_property] Property 'prop' is not part of start node target's a-node-target key and unique properties");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_refers_to_non_key_and_non_unique_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.relationships[0].end_node_reference.key_mappings[0].node_property] Property 'prop' is not part of end node target's a-node-target key and unique properties");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_start_node_reference_refers_to_node_key_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_end_node_reference_refers_to_node_key_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_start_node_reference_refers_to_node_unique_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_end_node_reference_refers_to_node_unique_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_does_not_refer_to_node_key_or_unique_property(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "[$.targets.relationships[0].start_node_reference] Node a-node-target must define a key or unique constraint for property id, none found",
                        "[$.targets.relationships[0].end_node_reference] Node a-node-target must define a key or unique constraint for property id, none found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_refers_to_node_without_keys_nor_unique_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].start_node_reference] Node a-node-target must define a key or unique constraint for property id, none found",
                        "[$.targets.relationships[0].end_node_reference] Node a-node-target must define a key or unique constraint for property id, none found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_different_indexes_and_constraints_use_same_name(SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Constraint or index name \"a-name\" must be defined at most once but 3 occurrences were found.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void pass_when_shared_name_indexes_and_constraints_are_equivalent(SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fail_when_shared_name_indexes_and_constraints_are_almost_equivalent(SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Constraint or index name \"a-name\" must be defined at most once but 2 occurrences were found.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_version_below_5_9_with_node_type_constraint(SpecFormat format, TestInfo testInfo) {
        assertThatThrownBy(() -> deserialize(
                        specReader(format, testInfo),
                        Neo4jDistributions.enterprise().of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] type_constraints are not supported by Neo4j 5.0 ENTERPRISE.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_edition_community_with_node_key_constraint(SpecFormat format, TestInfo testInfo) {
        assertThatThrownBy(() -> deserialize(
                        specReader(format, testInfo),
                        Neo4jDistributions.community().of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] key_constraints are not supported by Neo4j 5.0 COMMUNITY.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_version_below_5_13_with_node_vector_index(SpecFormat format, TestInfo testInfo) {
        assertThatThrownBy(() -> deserialize(
                        specReader(format, testInfo),
                        Neo4jDistributions.enterprise().of("5.12")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] vector_indexes are not supported by Neo4j 5.12 ENTERPRISE.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_version_below_5_9_with_relationship_type_constraint(SpecFormat format, TestInfo testInfo) {
        assertThatThrownBy(() -> deserialize(
                        specReader(format, testInfo),
                        Neo4jDistributions.enterprise().of("5.8")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].schema] type_constraints are not supported by Neo4j 5.8 ENTERPRISE.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_below_the_version_with_multiple_nodes_and_relationships(SpecFormat format, TestInfo testInfo) {
        assertThatThrownBy(() -> deserialize(
                        specReader(format, testInfo),
                        Neo4jDistributions.community().of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "4 error(s)",
                        "0 warning(s)",
                        "[VERS-001][$.targets.nodes[0].schema] type_constraints, key_constraints, existence_constraints are not supported by Neo4j 5.0 COMMUNITY.",
                        "[VERS-001][$.targets.nodes[1].schema] key_constraints, vector_indexes are not supported by Neo4j 5.0 COMMUNITY.",
                        "[VERS-001][$.targets.relationships[0].schema] type_constraints are not supported by Neo4j 5.0 COMMUNITY.",
                        "[VERS-001][$.targets.relationships[1].schema] vector_indexes are not supported by Neo4j 5.0 COMMUNITY.");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_above_the_version(SpecFormat format, TestInfo testInfo) {
        assertDoesNotThrow(() -> deserialize(
                specReader(format, testInfo), Neo4jDistributions.enterprise().of("5.20")));
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_active_attribute_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].active: integer found, boolean expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_source_attribute_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'source' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_source_attribute_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].source: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_source_attribute_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.nodes[0].source: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_source_attribute_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].source: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_write_mode_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.nodes[0].write_mode: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_write_mode_has_wrong_value(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].write_mode: does not have a value in the enumeration");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'labels' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].labels: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].labels[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.nodes[0].labels[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_labels_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_source_field_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: required property 'source_field' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_source_field_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_source_field_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_source_field_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: required property 'target_property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_type_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property_type: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_target_property_mappings_target_property_type_is_unsupported(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property_type: does not have a value in the enumeration");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].schema: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraints_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraints_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_type_constraint_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraints_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraints_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_existence_constraint_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraints_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraints_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_properties_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_properties_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_properties_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_properties_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_properties_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_properties_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_unique_constraint_options_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraints_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraints_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_properties_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_properties_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_properties_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_properties_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_properties_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_properties_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_key_constraint_options_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_indexes_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_properties_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_properties_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_properties_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_properties_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_properties_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_range_index_properties_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_indexes_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_text_index_options_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_indexes_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_properties_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_point_index_options_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_indexes_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'labels' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_is_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[DUPL-010][$.targets.nodes[0].schema.fulltext_indexes[0].label] $.targets.nodes[0].schema.fulltext_indexes[0].label \"Label\" must be defined at most once but 3 occurrences were found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_labels_is_dangling_and_duplicated(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "3 error(s)",
                        "0 warning(s)",
                        "[DANG-019][$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]] $.targets.nodes[0].schema.fulltext_indexes[0].labels[0] \"Label1\" is not part of the defined labels",
                        "[DANG-019][$.targets.nodes[0].schema.fulltext_indexes[0].labels[1]] $.targets.nodes[0].schema.fulltext_indexes[0].labels[1] \"Label1\" is not part of the defined labels",
                        "[DANG-019][$.targets.nodes[0].schema.fulltext_indexes[0].labels[2]] $.targets.nodes[0].schema.fulltext_indexes[0].labels[2] \"Label1\" is not part of the defined labels")
                .hasMessageNotContaining("[DUPL-010]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_properties_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_properties_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_properties_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_properties_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_properties_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_fulltext_index_options_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_indexes_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_label_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'label' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_label_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_label_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_label_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_options_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'options' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_schema_vector_index_options_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_key_and_existence_constraints_are_defined_on_same_labels_and_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_existence_constraints_are_defined_on_same_properties_but_different_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_node_key_constraint_overlap_with_node_existence_properties(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_existence_constraints_properties_overlap_but_reference_different_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_existence_constraints_are_defined_on_same_label_but_different_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_key_and_existence_constraint_define_invalid_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_key_and_existence_constraint_defines_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_key_and_unique_constraints_are_defined_on_same_labels_and_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and unique constraints: unique_constraints[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_properties_but_different_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    void does_not_fail_if_node_key_and_unique_constraints_are_defined_on_same_properties_in_different_order(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_node_key_and_unique_constraints_only_share_property_subset(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_label_but_different_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_key_and_unique_constraints_define_invalid_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"not-a-label\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_node_key_and_unique_constraints_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_key_constraint_and_range_index_are_defined_on_same_labels_and_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key constraint and range index: range_indexes[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_properties_but_different_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    void does_not_fail_if_node_key_constraint_and_range_index_are_defined_on_same_properties_in_different_order(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_node_key_constraint_and_range_index_only_share_property_subset(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_label_but_different_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_key_constraint_and_range_index_define_invalid_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"not-a-label\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_node_key_constraint_and_range_index_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_unique_constraint_and_range_index_are_defined_on_same_labels_and_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant unique constraint and range index: range_indexes[0], unique_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_properties_but_different_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    void does_not_fail_if_node_unique_constraint_and_range_index_are_defined_on_same_properties_in_different_order(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_node_unique_constraint_and_range_index_only_share_property_subset(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_label_but_different_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_unique_constraint_and_range_index_define_invalid_labels(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"not-a-label\" is not part of the defined labels");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_node_unique_constraint_and_range_index_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_full_start_and_end_node_references(SpecFormat format, TestInfo testInfo) throws Exception {

        try (var reader = specReader(format, testInfo)) {
            var spec = deserialize(reader);

            var relationships = spec.getTargets().getRelationships();
            assertThat(relationships).hasSize(1);
            var relationship = relationships.get(0);
            var startNode = relationship.getStartNodeReference();
            assertThat(startNode.getName()).isEqualTo("movie");
            var startKeyMappings = startNode.getKeyMappings();
            assertThat(startKeyMappings).hasSize(1);
            var startKeyMapping = startKeyMappings.get(0);
            assertThat(startKeyMapping.getSourceField()).isEqualTo("movie_id");
            assertThat(startKeyMapping.getNodeProperty()).isEqualTo("identifier");
            var endNode = relationship.getEndNodeReference();
            assertThat(endNode.getName()).isEqualTo("category");
            assertThat(endNode.getKeyMappings()).hasSize(1);
            var endKeyMapping = endNode.getKeyMappings().get(0);
            assertThat(endKeyMapping.getSourceField()).isEqualTo("category_id");
            assertThat(endKeyMapping.getNodeProperty()).isEqualTo("identifier");
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_active_attribute_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].active: integer found, boolean expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_source_attribute_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0]: required property 'source' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_source_attribute_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].source: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_source_attribute_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].source: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_source_attribute_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].source: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_write_mode_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].write_mode: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_write_mode_has_wrong_value(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].write_mode: does not have a value in the enumeration");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_node_match_mode_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].node_match_mode: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_node_match_mode_has_wrong_value(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].node_match_mode: does not have a value in the enumeration");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_name_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: required property 'key_mappings' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_source_field_is_missing(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0]: required property 'source_field' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_source_field_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].source_field: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_source_field_is_empty(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].source_field: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_source_field_is_blank(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].source_field: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_node_property_is_missing(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0]: required property 'node_property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_node_property_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_node_property_is_empty(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_start_node_reference_key_mappings_node_property_is_blank(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_name_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: required property 'key_mappings' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_has_wrong_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_source_field_is_missing(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0]: required property 'source_field' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_source_field_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].source_field: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_source_field_is_empty(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].source_field: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_source_field_is_blank(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].source_field: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_target_property_is_missing(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0]: required property 'node_property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_node_property_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_node_property_is_empty(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_end_node_reference_key_mappings_node_property_is_blank(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_type_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships[0]: required property 'type' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_type_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].type: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_type_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].type: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_type_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].type: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_source_field_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0]: required property 'source_field' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_source_field_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_source_field_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_source_field_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_is_missing(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0]: required property 'target_property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_type_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property_type: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_target_property_mappings_target_property_type_is_unsupported(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property_type: does not have a value in the enumeration");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraints_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraints_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_type_constraint_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraints_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraints_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_properties_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_properties_are_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_properties_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_properties_are_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_properties_element_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_properties_element_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_key_constraint_options_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraints_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraints_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_properties_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_properties_are_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_properties_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_properties_are_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties: must have at least 1 items but found 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_properties_element_is_empty(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_properties_element_is_blank(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_unique_constraint_options_are_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraints_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraints_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_property_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_existence_constraint_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_indexes_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_properties_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_properties_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_properties_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_range_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_indexes_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_text_index_options_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_indexes_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_point_index_options_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_indexes_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_properties_are_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0]: required property 'properties' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_properties_are_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_properties_element_is_wrongly_typed(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0]: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_fulltext_index_options_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_indexes_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes: integer found, array expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_indexes_element_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0]: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_name_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0]: required property 'name' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_name_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].name: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_name_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].name: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_name_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].name: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_property_is_missing(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0]: required property 'property' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_property_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_property_is_empty(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property: must be at least 1 characters long");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_property_is_blank(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property: does not match the regex pattern \\S+");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_schema_vector_index_options_are_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].options: integer found, object expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_key_and_existence_constraints_are_defined_on_same_properties(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_relationship_key_constraint_overlap_with_relationship_existence_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_existence_constraints_are_not_defined_on_same_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_key_and_existence_constraints_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.existence_constraints[0].property \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_key_and_unique_constraints_are_defined_on_same_properties(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key and unique constraints: unique_constraints[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    void does_not_fail_if_relationship_key_and_unique_constraints_are_defined_on_same_properties_in_different_order(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_key_and_unique_constraints_only_share_property_subset(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_and_unique_constraints_are_not_defined_on_same_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_relationship_key_and_unique_constraints_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_key_constraint_and_range_index_are_defined_on_same_properties(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key constraint and range index: range_indexes[0], key_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    void does_not_fail_if_relationship_key_constraint_and_range_index_are_defined_on_same_properties_in_different_order(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_key_constraint_and_range_index_only_share_property_subset(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_key_constraint_and_range_index_are_not_defined_on_same_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_relationship_key_constraint_and_range_index_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_unique_constraint_and_range_index_are_defined_on_same_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant unique constraint and range index: range_indexes[0], unique_constraints[0]");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    void
            does_not_fail_if_relationship_unique_constraint_and_range_index_are_defined_on_same_properties_in_different_order(
                    SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_relationship_unique_constraint_and_range_index_only_share_property_subset(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_fail_if_unique_constraint_and_range_index_are_not_defined_on_same_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatCode(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_if_relationship_unique_constraint_and_range_index_define_invalid_properties(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void reports_redundancy_for_start_node_reference_also_declared_as_explicit_dependency(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].dependencies \"a-node-target\" is defined as an explicit dependency *and* as a start node reference, remove it from dependencies");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void reports_redundancy_for_end_node_reference_also_declared_as_explicit_dependency(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].dependencies \"another-node-target\" is defined as an explicit dependency *and* as an end node reference, remove it from dependencies");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void
            does_not_report_redundancy_for_node_reference_also_declared_as_explicit_dependency_if_a_node_reference_is_dangling(
                    SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].end_node_reference] $.targets.relationships[0].end_node_reference refers to a non-existing node target \"not-a-node-target\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void does_not_report_redundancy_for_node_reference_also_declared_as_explicit_dependency_if_a_dependency_is_dangling(
            SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0]] $.targets.relationships[0] depends on a non-existing target \"another-node-target\"");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void
            does_not_report_redundancy_for_node_reference_also_declared_as_explicit_dependency_if_a_dependency_is_duplicated(
                    SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].depends_on] $.targets.relationships[0].depends_on defines dependency \"a-node-target\" 2 times, it must be defined at most once");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_is_missing_type(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0]: required property 'type' not found");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_type_is_wrongly_typed(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0].type: integer found, string expected");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_source_type_is_not_supported_by_any_loaded_source_provider(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(UndeserializableSourceException.class)
                .hasMessageContainingAll(
                        "Expected exactly one source provider for sources of type unsupported, but found: 0");
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void fails_if_third_party_source_and_supplier_do_not_match(SpecFormat format, TestInfo testInfo) {

        assertThatThrownBy(() -> {
                    try (var reader = specReader(format, testInfo)) {
                        deserialize(reader);
                    }
                })
                .isInstanceOf(UndeserializableSourceException.class)
                .hasMessageContainingAll(
                        "Source provider org.neo4j.importer.v1.sources.JdbcSourceProvider failed to deserialize the following source definition");
    }

    private static FileReader specReader(SpecFormat format, TestInfo testInfo) {
        var spec = String.format(
                "/specs/import_specification_deserializer_test/%s.%s",
                testInfo.getTestMethod().orElseThrow().getName(), format.extension());
        var resourceUrl = ImportSpecificationDeserializerTest.class.getResource(spec);
        assertThat(resourceUrl).isNotNull();
        try {
            return new FileReader(Path.of(resourceUrl.toURI()).toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
