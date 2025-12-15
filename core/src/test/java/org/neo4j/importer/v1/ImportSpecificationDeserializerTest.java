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
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.neo4j.importer.v1.validation.SpecificationException;
import org.neo4j.importer.v1.validation.UndeserializableActionException;
import org.neo4j.importer.v1.validation.UndeserializableSourceException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

class ImportSpecificationDeserializerTest {

    @BeforeEach
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
    }

    @Test
    void deserializes_minimal_job_spec() throws Exception {
        var json = """
                            {
                                "version": "1",
                                "sources": [{
                                    "name": "my-bigquery-source",
                                    "type": "bigquery",
                                    "query": "SELECT id, name FROM my.table"
                                }],
                                "targets": {
                                    "queries": [{
                                        "name": "my-query",
                                        "source": "my-bigquery-source",
                                        "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                    }]
                                }
                            }
                        """.stripIndent();

        var spec = deserialize(new StringReader(json));

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

    @Test
    void deserializes_job_spec() throws Exception {
        var json = """
                            {
                                "version": "1",
                                "config": {
                                    "foo": "bar",
                                    "baz": 42,
                                    "qix": [true, 1.0, {}]
                                },
                                "sources": [{
                                    "name": "my-bigquery-source",
                                    "type": "bigquery",
                                    "query": "SELECT id, name FROM my.table"
                                }],
                                "targets": {
                                    "queries": [{
                                        "name": "my-query",
                                        "source": "my-bigquery-source",
                                        "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                    }]
                                },
                                "actions": [{
                                    "name": "my-cypher-action",
                                    "type": "cypher",
                                    "query": "CREATE (:Started)",
                                    "stage": "start"
                                }]
                            }
                        """.stripIndent();

        var spec = deserialize(new StringReader(json));

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

    @Test
    void deserializes_job_spec_with_uppercase_write_mode_and_property_type() throws Exception {
        var json = """
                            {
                                "version": "1",
                                "sources": [{
                                    "name": "my-bigquery-source",
                                    "type": "bigquery",
                                    "query": "SELECT id, name FROM my.table"
                                }],
                                "targets": {
                                    "nodes": [{
                                        "name": "my-node",
                                        "source": "my-bigquery-source",
                                        "write_mode": "CREATE",
                                        "labels": ["ALabel"],
                                        "properties": [
                                            {
                                                "source_field": "id",
                                                "target_property": "id",
                                                "target_property_type": "STRING"
                                            },
                                        ]
                                    }]
                                }
                            }
                        """.stripIndent();

        var spec = deserialize(new StringReader(json));

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

    @Test
    void fails_if_spec_is_unparseable() {
        assertThatThrownBy(() -> deserialize(new StringReader("&*not-json-not-yaml*&")))
                .isInstanceOf(UnparseableSpecificationException.class);
    }

    @Test
    void fails_if_spec_is_not_deserializable() {
        assertThatThrownBy(() -> deserialize(new StringReader("[]")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: array found, object expected");
    }

    @Test
    void fails_if_version_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'version' not found");
    }

    @Test
    void fails_if_version_is_invalid() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": [],
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.version: must be the constant value '1'");
    }

    @Test
    void fails_if_sources_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'sources' not found");
    }

    @Test
    void fails_if_sources_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": 42,
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources: integer found, array expected");
    }

    @Test
    void fails_if_source_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [42],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0]: integer found, object expected");
    }

    @Test
    void fails_if_sources_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources: must have at least 1 items but found 0");
    }

    @Test
    void fails_if_targets_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'targets' not found");
    }

    @Test
    void fails_if_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": 42
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.targets: integer found, object expected");
    }

    @Test
    void fails_if_targets_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {}
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "3 error(s)",
                        "0 warning(s)",
                        "$.targets: required property 'nodes' not found",
                        "$.targets: required property 'relationships' not found",
                        "$.targets: required property 'queries' not found");
    }

    @Test
    void fails_if_node_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": 42
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes: integer found, array expected");
    }

    @Test
    void fails_if_relationship_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "relationships": 42
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships: integer found, array expected");
    }

    @Test
    void fails_if_custom_query_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": 42
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.queries: integer found, array expected");
    }

    @Test
    void fails_if_actions_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": 42
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions: integer found, array expected");
    }

    @Test
    void fails_if_action_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [42]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions[0]: integer found, object expected");
    }

    @Test
    void fails_if_name_is_missing_in_source() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_node_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_relationship_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "node_match_mode": "match",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.queries[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_action() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "type": "http",
                                "method": "get",
                                "stage": "start",
                                "url": "https://example.com"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_blank_in_source() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "  ",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_node_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "  ",
                                    "source": "a-source",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_relationship_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "   ",
                                    "source": "a-source",
                                    "write_mode": "create",
                                    "node_match_mode": "merge",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "   ",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_action() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "   ",
                                "type": "http",
                                "method": "get",
                                "stage": "post_nodes",
                                "url": "https://example.com"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_action_in_array_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [42]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions[0]: integer found, object expected");
    }

    @Test
    void fails_if_action_active_attribute_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "cypher",
                                "active": 42,
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].active: integer found, boolean expected");
    }

    @Test
    void fails_if_action_is_missing_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0]: required property 'type' not found");
    }

    @Test
    void fails_if_action_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": 42,
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0].type: integer found, string expected");
    }

    @Test
    void fails_if_action_type_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "foobar",
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining("Expected exactly one action provider for action of type foobar, but found: 0");
    }

    @Test
    void fails_if_action_stage_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "cypher",
                                "stage": 42,
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0].stage: integer found, string expected");
    }

    @Test
    void fails_if_action_stage_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "cypher",
                                "stage": "foobar",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].stage: does not have a value in the enumeration");
    }

    @Test
    void fails_if_cypher_action_execution_mode_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "stage": "start",
                                "type": "cypher",
                                "query": "RETURN 42",
                                "execution_mode": 42
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining(
                        "Action provider org.neo4j.importer.v1.actions.plugin.CypherActionProvider failed to deserialize");
    }

    @Test
    void fails_if_cypher_action_execution_mode_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "stage": "start",
                                "type": "cypher",
                                "query": "RETURN 42",
                                "execution_mode": "foobar"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining(
                        "Action provider org.neo4j.importer.v1.actions.plugin.CypherActionProvider failed to deserialize");
    }

    @Test
    void fails_if_source_name_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "duplicate",
                                "query": "SELECT id, name FROM my.table"
                            },
                            {
                                "type": "bigquery",
                                "name": "duplicate",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "target",
                                    "source": "duplicate",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.sources[1].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_target_name() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "duplicate",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "duplicate",
                                    "source": "duplicate",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.targets.nodes[0].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_action_name() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "duplicate",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "target",
                                    "source": "duplicate",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            },
                            "actions": [{
                                "name": "duplicate",
                                "type": "cypher",
                                "stage": "pre_relationships",
                                "query": "CREATE (:PreRel)"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.actions[0].name");
    }

    @Test
    void fails_if_node_target_name_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "duplicate",
                                    "source": "source",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                },{
                                    "name": "duplicate",
                                    "source": "source",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.nodes[1].name");
    }

    @Test
    void fails_if_node_target_name_is_duplicated_with_rel_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "source",
                                    "name": "duplicate",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "duplicate",
                                    "source": "source",
                                    "type": "TYPE",
                                    "start_node_reference": "duplicate",
                                    "end_node_reference": "duplicate"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.relationships[0].name");
    }

    @Test
    void fails_if_node_target_name_is_duplicated_with_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "source",
                                    "name": "duplicate",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "queries": [{
                                    "name": "duplicate",
                                    "source": "source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.queries[0].name");
    }

    @Test
    void fails_if_query_target_name_is_duplicated_with_action() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "duplicate",
                                    "source": "source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "duplicate",
                                "type": "cypher",
                                "stage": "pre_relationships",
                                "query": "CREATE (:PreRel)"
                            }]
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.queries[0].name, $.actions[0].name");
    }

    @Test
    void fails_if_node_target_does_not_refer_to_existing_source() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-target",
                                    "source": "incorrect-source-name",
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @Test
    void fails_if_relationship_target_does_not_refer_to_existing_source() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "incorrect-source-name",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @Test
    void fails_if_custom_query_target_does_not_refer_to_existing_source() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "incorrect-source-name",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @Test
    void fails_if_node_target_depends_on_non_existing_target() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "depends_on": ["invalid"],
                                    "labels": ["Label1", "Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] depends on a non-existing target \"invalid\"");
    }

    @Test
    void fails_if_relationship_target_depends_on_non_existing_target_or_action() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-key-constraint",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "depends_on": ["invalid"],
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0] depends on a non-existing target \"invalid\"");
    }

    @Test
    void fails_if_custom_query_target_depends_on_non_existing_target_or_action() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "depends_on": ["invalid"],
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0] depends on a non-existing target \"invalid\"");
    }

    @Test
    void fails_if_relationship_refers_to_a_non_existing_node_target_for_start() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "incorrect-reference",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @Test
    void fails_if_active_relationship_target_refers_to_an_inactive_node_target_for_start() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": false,
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                },{
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label3"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference belongs to an active target but refers to an inactive node target \"a-node-target\"");
    }

    @Test
    void fails_if_active_relationship_target_refers_to_an_inactive_node_target_for_end() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-key-constraint",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "active": false,
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label3"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "another-key-constraint",
                                            "label": "Label3",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference belongs to an active target but refers to an inactive node target \"another-node-target\"");
    }

    @Test
    void does_not_fail_if_inactive_relationship_refers_to_an_inactive_node_target_for_start() {
        assertThatNoException().isThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": false,
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label3"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "another-key-constraint",
                                           "label": "Label3",
                                           "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "active": false,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target"
                                }]
                            }
                        }
                        """.stripIndent())));
    }

    @Test
    void does_not_fail_if_inactive_relationship_refers_to_an_inactive_node_target_for_end() {
        assertThatNoException().isThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint1",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "active": false,
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label3"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint2",
                                           "label": "Label3",
                                           "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "active": false,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target"
                                }]
                            }
                        }
                        """.stripIndent())));
    }

    @Test
    void does_not_report_inactive_node_reference_when_other_references_are_dangling() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": false,
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label3"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "incorrect-reference",
                                    "end_node_reference": "another-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @Test
    void fails_if_relationship_refers_to_a_non_existing_node_target_for_end() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "incorrect-reference"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @Test
    void fails_if_direct_dependency_cycle_is_detected() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "depends_on": ["a-target"],
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "A dependency cycle has been detected: a-target->a-target");
    }

    @Test
    void fails_if_longer_dependency_cycle_is_detected() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label1",
                                         "properties": ["property1"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "depends_on": ["a-query-target"],
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }],
                                "queries": [
                                    {
                                        "name": "a-query-target",
                                        "source": "a-source",
                                        "depends_on": ["another-query-target"],
                                        "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                    },
                                    {
                                        "name": "another-query-target",
                                        "source": "a-source",
                                        "depends_on": ["a-relationship-target"],
                                        "query": "UNWIND $rows AS row CREATE (n:AnotherNode) SET n = row"
                                    }
                                ]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-relationship-target->a-query-target->another-query-target->a-relationship-target");
    }

    @Test
    void fails_if_dependency_cycle_is_detected_via_start_node_reference() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name, description FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "depends_on": ["a-relationship-target"],
                                    "labels": ["Label1"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                },
                                {
                                    "name": "another-node-target",
                                    "source": "a-source",
                                    "labels": ["Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "another-key-constraint",
                                           "label": "Label2",
                                           "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-node-target->a-relationship-target->a-node-target");
    }

    @Test
    void fails_if_dependency_cycle_is_detected_via_end_node_reference() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name, description FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "depends_on": ["a-relationship-target"],
                                    "labels": ["Label1"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "name": "another-node-target",
                                    "source": "a-source",
                                    "labels": ["Label2"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-node-target->a-relationship-target->a-node-target");
    }

    @Test
    void does_not_report_cycles_if_names_are_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "depends_on": ["a-target"],
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }],
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "depends_on": ["a-target"],
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"a-target\" is duplicated across the following paths: $.targets.relationships[0].name, $.targets.queries[0].name");
    }

    @Test
    void does_not_report_cycles_if_depends_on_are_dangling() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-key-constraint",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "depends_on": ["a-query-target"],
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }],
                                "queries": [{
                                    "name": "a-query-target",
                                    "source": "a-source",
                                    "depends_on": ["a-relationship-target"],
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                },
                                {
                                    "name": "another-query-target",
                                    "source": "a-source",
                                    "depends_on": ["invalid", "a-relationship-target"],
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[1] depends on a non-existing target \"invalid\"");
    }

    @Test
    void does_not_report_cycles_if_node_references_are_dangling() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name, description FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "depends_on": ["a-relationship-target"],
                                    "labels": ["Label1"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                },{
                                    "name": "a-relationship-target-2",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "invalid-ref",
                                    "end_node_reference": "a-node-target"
                                }
                                ]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[1].start_node_reference refers to a non-existing node target \"invalid-ref\"");
    }

    @Test
    void fails_if_all_targets_are_inactive() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                              "name": "a-source",
                              "type": "text",
                              "header": [
                                "column1",
                                "column2"
                              ],
                              "data": [
                                [
                                  "value1", "value2"
                                ]
                              ]
                            }],
                            "targets": {
                                "queries": [{
                                    "active": false,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "[$.targets] at least one target must be active, none found");
    }

    @Test
    void fails_if_node_target_duplicates_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [
                            {
                              "name": "a-source",
                              "type": "text",
                              "header": [
                                "column1",
                                "column2"
                              ],
                              "data": [
                                [
                                  "value1", "value2"
                                ]
                              ]
                            }
                            ],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label1", "Label2"],
                                    "depends_on": ["a-query-target", "a-query-target"],
                                    "write_mode": "create",
                                    "properties": [
                                        {"source_field": "column1", "target_property": "property1"},
                                        {"source_field": "column2", "target_property": "property2"}
                                    ]
                                }],
                                "queries": [{
                                    "name": "a-query-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].depends_on defines dependency \"a-query-target\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_relationship_target_duplicates_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [
                            {
                              "name": "a-source",
                              "type": "text",
                              "header": [
                                "column1",
                                "column2"
                              ],
                              "data": [
                                [
                                  "value1", "value2"
                                ]
                              ]
                            }
                            ],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "column1", "target_property": "property1"},
                                        {"source_field": "column2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                           "name": "a-key-constraint",
                                           "label": "Label1",
                                           "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "depends_on": ["a-query-target", "a-query-target"],
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }],
                                "queries": [{
                                    "name": "a-query-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].depends_on defines dependency \"a-query-target\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_query_target_duplicates_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [
                            {
                              "name": "a-source",
                              "type": "text",
                              "header": [
                                "column1",
                                "column2"
                              ],
                              "data": [
                                [
                                  "value1", "value2"
                                ]
                              ]
                            }
                            ],
                            "targets": {
                                "queries": [{
                                    "name": "a-query-target",
                                    "source": "a-source",
                                    "depends_on": ["a-relationship-target", "a-relationship-target"],
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }],
                               "nodes": [{
                                   "source": "a-source",
                                   "name": "a-node-target",
                                   "write_mode": "merge",
                                   "labels": ["Label1", "Label2"],
                                   "properties": [
                                       {"source_field": "column1", "target_property": "property1"},
                                       {"source_field": "column2", "target_property": "property2"}
                                   ],
                                   "schema": {
                                       "key_constraints": [{
                                          "name": "a-key-constraint",
                                          "label": "Label1",
                                          "properties": ["property1"]
                                       }]
                                   },
                               }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @Test
    void does_not_report_cycles_if_targets_duplicate_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [
                            {
                              "name": "a-source",
                              "type": "text",
                              "header": [
                                "column1",
                                "column2"
                              ],
                              "data": [
                                [
                                  "value1", "value2"
                                ]
                              ]
                            }
                            ],
                            "targets": {
                                "queries": [{
                                    "name": "a-query-target",
                                    "source": "a-source",
                                    "depends_on": ["a-relationship-target", "a-relationship-target"],
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }],
                               "nodes": [{
                                   "source": "a-source",
                                   "name": "a-node-target",
                                   "write_mode": "merge",
                                   "labels": ["Label1", "Label2"],
                                   "properties": [
                                       {"source_field": "column1", "target_property": "property1"},
                                       {"source_field": "column2", "target_property": "property2"}
                                   ],
                                   "schema": {
                                       "key_constraints": [{
                                          "name": "a-key-constraint",
                                          "label": "Label1",
                                          "properties": ["property1"]
                                       }]
                                   }
                               }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "depends_on": ["a-query-target"],
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @Test
    public void fails_if_node_target_labels_are_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label1", "Label1", "Label2", "Label2", "Label2"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels[0] \"Label1\" must be defined only once but found 2 occurrences",
                        "$.targets.nodes[0].labels[2] \"Label2\" must be defined only once but found 3 occurrences");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                          {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "active": true,
                              "name": "a-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "property"},
                                {"source_field": "name", "target_property": "property"}
                              ]
                            }]
                          }
                        }""".stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                  "key_constraints": [{
                                     "name": "a-key-constraint",
                                     "label": "Label",
                                     "properties": ["id"]
                                  }]
                              }
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": "property"},
                                {"source_field": "name", "target_property": "property"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
    }

    @Test
    public void fails_if_node_target_type_constraint_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id", "target_property_type": "integer"}
                              ],
                              "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Label", "property": "invalid"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_type_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id", "target_property_type": "integer"}
                              ],
                              "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Invalid", "property": "id"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_type_constraint_property_refers_to_an_untyped_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "untyped"}
                              ],
                              "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Label", "property": "untyped"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property \"untyped\" refers to an untyped property");
    }

    @Test
    public void fails_if_relationship_target_type_constraint_property_refers_to_an_untyped_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""

                                {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "unique_constraints": [
                                  {"name": "a unique constraint", "label": "Label", "properties": ["id"]}
                                ]
                              }
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "SELF_LINKS_TO",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": "untyped"}
                              ],
                              "schema": {
                                "type_constraints": [
                                  {"name": "a type constraint", "property": "untyped"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property \"untyped\" refers to an untyped property");
    }

    @Test
    public void fails_if_node_target_unique_constraint_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["invalid"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_unique_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Invalid", "properties": ["id"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_key_constraint_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["invalid"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_key_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Invalid", "properties": ["id"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_existence_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Invalid", "property": "id"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_existence_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Label", "property": "invalid"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_range_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "range_indexes": [
                                    {"name": "a range index", "label": "Invalid", "properties": ["id"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_range_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["invalid"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_text_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Invalid", "property": "id"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_text_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Label", "property": "invalid"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_point_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "point_indexes": [
                                    {"name": "a point index", "label": "Invalid", "property": "id"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_point_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "point_indexes": [
                                    {"name": "a point index", "label": "Label", "property": "invalid"}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_fulltext_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "fulltext_indexes": [
                                    {"name": "a full text index", "labels": ["Invalid"], "properties": ["id"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0] \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_fulltext_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "fulltext_indexes": [
                                    {"name": "a full text index", "labels": ["Label"], "properties": ["invalid"]}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_vector_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "vector_indexes": [
                                    {"name": "a vector index", "label": "Invalid", "property": "id", "options": {}}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_vector_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ],
                              "schema": {
                                "vector_indexes": [
                                    {"name": "a vector index", "label": "Label", "property": "invalid", "options": {}}
                                ]
                              }
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_type_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [{
                                        "name": "a type constraint", "property": "invalid"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_key_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [{
                                        "name": "a key constraint", "properties": ["invalid"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_unique_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [{
                                        "name": "a unique constraint", "properties": ["invalid"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_existence_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                     "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                     }]
                                  }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [{
                                        "name": "an existence constraint", "property": "invalid"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_range_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [{
                                        "name": "a range index", "properties": ["invalid"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_text_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [{
                                        "name": "a text index", "property": "invalid"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_point_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [{
                                        "name": "a point index", "property": "invalid"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_fulltext_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [{
                                        "name": "a fulltext index", "properties": ["invalid"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_vector_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                "schema": {
                                    "key_constraints": [{
                                       "name": "a-key-constraint",
                                       "label": "Label",
                                       "properties": ["id"]
                                    }]
                                }
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [{
                                        "name": "a vector index", "property": "invalid", "options": {}
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_start_node_reference_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "not-a-valid-property"
                                            }
                                        ]
                                    },
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property refers to a non-existing node property \"not-a-valid-property\"");
    }

    @Test
    public void fails_if_relationship_end_node_reference_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-key-constraint",
                                            "label": "Label",
                                            "properties": ["id"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "not-a-valid-property"
                                            }
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property refers to a non-existing node property \"not-a-valid-property\"");
    }

    @Test
    public void fails_if_relationship_start_node_reference_refers_to_non_key_and_non_unique_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"},
                                        {"source_field": "field", "target_property": "prop"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-key-constraint",
                                            "label": "Label",
                                            "properties": ["id"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "prop"
                                            }
                                        ]
                                    },
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.relationships[0].start_node_reference.key_mappings[0].node_property] Property 'prop' is not part of start node target's a-node-target key and unique properties");
    }

    @Test
    public void fails_if_relationship_end_node_reference_refers_to_non_key_and_non_unique_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"},
                                        {"source_field": "field", "target_property": "prop"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-key-constraint",
                                            "label": "Label",
                                            "properties": ["id"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "prop"
                                            }
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.relationships[0].end_node_reference.key_mappings[0].node_property] Property 'prop' is not part of end node target's a-node-target key and unique properties");
    }

    @Test
    public void does_not_fail_if_relationship_start_node_reference_refers_to_node_key_property() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "unique-id", "label": "Label", "properties": ["id"]}
                                        ]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "id"
                                            }
                                        ]
                                    },
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_end_node_reference_refers_to_node_key_property() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "unique-id", "label": "Label", "properties": ["id"]}
                                        ]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "id"
                                            }
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_start_node_reference_refers_to_node_unique_property() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "unique-id", "label": "Label", "properties": ["id"]}
                                        ]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "id"
                                            }
                                        ]
                                    },
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_end_node_reference_refers_to_node_unique_property() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "unique-id", "label": "Label", "properties": ["id"]}
                                        ]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "id"
                                            }
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void fails_if_relationship_start_node_reference_does_not_refer_to_node_key_or_unique_property() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "id"
                                            }
                                        ]
                                    },
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "[$.targets.relationships[0].start_node_reference] Node a-node-target must define a key or unique constraint for property id, none found",
                        "[$.targets.relationships[0].end_node_reference] Node a-node-target must define a key or unique constraint for property id, none found");
    }

    @Test
    public void fails_if_relationship_end_node_reference_refers_to_node_without_keys_nor_unique_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": {
                                        "name": "a-node-target",
                                        "key_mappings": [
                                            {
                                                "source_field": "source_id",
                                                "node_property": "id"
                                            }
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].start_node_reference] Node a-node-target must define a key or unique constraint for property id, none found",
                        "[$.targets.relationships[0].end_node_reference] Node a-node-target must define a key or unique constraint for property id, none found");
    }

    @Test
    void fails_if_different_indexes_and_constraints_use_same_name() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-name",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label3"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-name",
                                            "label": "Label3",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"}
                                    ],
                                    "schema": {
                                        "range_indexes": [{
                                            "name": "a-name",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Constraint or index name \"a-name\" must be defined at most once but 3 occurrences were found.");
    }

    @Test
    void pass_when_shared_name_indexes_and_constraints_are_equivalent() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-name",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-name",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"}
                                    ],
                                    "schema": {
                                        "range_indexes": [{
                                            "name": "a-relationship-range-index",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }]
                            }
                        }
                        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    void fail_when_shared_name_indexes_and_constraints_are_almost_equivalent() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "type": "bigquery",
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "source": "a-source",
                                    "name": "a-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1", "Label2"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [{
                                            "name": "a-name",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                },{
                                    "source": "a-source",
                                    "name": "another-node-target",
                                    "write_mode": "merge",
                                    "labels": ["Label1"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"},
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [{
                                            "name": "a-name",
                                            "label": "Label1",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }],
                                "relationships": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"}
                                    ],
                                    "schema": {
                                        "range_indexes": [{
                                            "name": "a-relationship-range-index",
                                            "properties": ["property1"]
                                        }]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Constraint or index name \"a-name\" must be defined at most once but 2 occurrences were found.");
    }

    @Test
    public void fails_if_version_below_5_9_with_node_type_constraint() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader("""
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                ]
                            }
                        }
                    ]
                }
            }
            """.stripIndent()),
                        Neo4jDistributions.enterprise().of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] type_constraints are not supported by Neo4j 5.0 ENTERPRISE.");
    }

    @Test
    public void fails_if_edition_community_with_node_key_constraint() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader("""
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"},
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "key_constraint_1", "label": "Label1", "properties": ["property1"]}
                                ]
                            }
                        }
                    ]
                }
            }
            """.stripIndent()),
                        Neo4jDistributions.community().of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] key_constraints are not supported by Neo4j 5.0 COMMUNITY.");
    }

    @Test
    public void fails_if_version_below_5_13_with_node_vector_index() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader("""
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"}
                            ],
                            "schema": {
                                "vector_indexes": [
                                    {"name": "vector_index_1", "label": "Label1", "property": "property1", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                ]
                            }
                        }
                    ]
                }
            }
            """.stripIndent()),
                        Neo4jDistributions.enterprise().of("5.12")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] vector_indexes are not supported by Neo4j 5.12 ENTERPRISE.");
    }

    @Test
    public void fails_if_version_below_5_9_with_relationship_type_constraint() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader("""
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"}
                            ],
                            "schema": {
                               "key_constraints": [{
                                  "name": "a-key-constraint",
                                  "label": "Label1",
                                  "properties": ["property1"]
                               }]
                            }
                        },
                        {
                            "name": "another-node-target",
                            "source": "a-source",
                            "labels": ["Label2"],
                            "properties": [
                                {"source_field": "field_2", "target_property": "property2", "target_property_type": "integer"}
                            ],
                            "schema": {
                               "key_constraints": [{
                                  "name": "another-key-constraint",
                                  "label": "Label2",
                                  "properties": ["property2"]
                               }]
                            }
                        }
                    ],
                    "relationships": [
                        {
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "another-node-target",
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_1", "property": "property1"}
                                ]
                            }
                        }
                    ]
                }
            }

            """.stripIndent()),
                        Neo4jDistributions.enterprise().of("5.8")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].schema] type_constraints are not supported by Neo4j 5.8 ENTERPRISE.");
    }

    @Test
    public void fails_if_below_the_version_with_multiple_nodes_and_relationships() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader("""
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"},
                                {"source_field": "field_2", "target_property": "property2", "target_property_type": "string"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "key_constraint_1", "label": "Label1", "properties": ["property2"]}
                                ],
                                "type_constraints": [
                                    {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                ],
                                "existence_constraints": [
                                    {"name": "existence_constraint_1", "label": "Label1", "property": "property1"}
                                ]
                            }
                        },
                        {
                            "name": "another-node-target",
                            "source": "a-source",
                            "labels": ["Label2"],
                            "properties": [
                                {"source_field": "field_2", "target_property": "property2", "target_property_type": "integer"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "key_constraint_2", "label": "Label2", "properties": ["property2"]}
                                ],
                                "vector_indexes": [
                                    {"name": "vector_index_1", "label": "Label2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                ]
                            }
                        }
                    ],
                    "relationships": [
                        {
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "another-node-target",
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_2", "property": "property1"}
                                ]
                            }
                        },
                        {
                            "name": "another-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "another-node-target",
                            "properties": [
                                {"source_field": "field_2", "target_property": "property2", "target_property_type": "integer"}
                            ],
                            "schema": {
                                "vector_indexes": [
                                    {"name": "vector_index_2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                ]
                            }
                        }
                    ]
                }
            }
            """.stripIndent()),
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

    @Test
    public void does_not_fail_if_above_the_version() {
        assertDoesNotThrow(() -> deserialize(
                new StringReader("""
                    {
                        "version": "1",
                        "sources": [
                            {
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }
                        ],
                        "targets": {
                            "nodes": [
                                {
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label1"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"},
                                        {"source_field": "field_2", "target_property": "property2", "target_property_type": "integer"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                           {"name": "key_constraint_1", "label": "Label1", "properties": ["property2"]}
                                        ],
                                        "type_constraints": [
                                            {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                        ],
                                        "existence_constraints": [
                                            {"name": "existence_constraint_1", "label": "Label1", "property": "property1"}
                                        ]
                                    }
                                },
                                {
                                    "name": "another-node-target",
                                    "source": "a-source",
                                    "labels": ["Label2"],
                                    "properties": [
                                        {"source_field": "field_2", "target_property": "property2", "target_property_type": "integer"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                           {"name": "key_constraint_2", "label": "Label2", "properties": ["property2"]}
                                        ],
                                        "vector_indexes": [
                                            {"name": "vector_index_1", "label": "Label2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                        ]
                                    }
                                }
                            ],
                            "relationships": [
                                {
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"}
                                    ],
                                    "schema": {
                                        "type_constraints": [
                                            {"name": "type_constraint_2", "property": "property1"}
                                        ]
                                    }
                                },
                                {
                                    "name": "another-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_2", "target_property": "property2", "target_property_type": "integer"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "vector_index_2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                    """.stripIndent()),
                Neo4jDistributions.enterprise().of("5.20")));
    }

    @Test
    public void fails_if_node_target_active_attribute_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": 42,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].active: integer found, boolean expected");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'source' not found");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": 42,
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].source: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.nodes[0].source: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "   ",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].source: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_write_mode_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": 42,
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.nodes[0].write_mode: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_write_mode_has_wrong_value() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "reticulating_splines",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].write_mode: does not have a value in the enumeration");
    }

    @Test
    public void fails_if_node_target_labels_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'labels' not found");
    }

    @Test
    public void fails_if_node_target_labels_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": 42,
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].labels: integer found, array expected");
    }

    @Test
    public void fails_if_node_target_labels_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": [],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_target_labels_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": [42],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].labels[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_labels_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": [""],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.nodes[0].labels[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_labels_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["   "],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_property_mappings_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_target_property_mappings_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": 42
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [42]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: required property 'source_field' not found");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": 42, "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "   ", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id"}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: required property 'target_property' not found");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": 42}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": ""}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "   "}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property", "target_property_type": 42}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property_type: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_type_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                        "version": "1",
                        "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                        }],
                        "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property", "target_property_type": "blackhole"}
                            ]
                        }]
                        }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property_type: does not have a value in the enumeration");
    }

    @Test
    public void fails_if_node_schema_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": 42
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].schema: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_type_constraints_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "active": true,
                    "name": "a-target",
                    "source": "a-source",
                    "write_mode": "merge",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "field", "target_property": "property"}
                    ],
                    "schema": {
                        "type_constraints": 42
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_type_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "active": true,
                    "name": "a-target",
                    "source": "a-source",
                    "write_mode": "merge",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "field", "target_property": "property"}
                    ],
                    "schema": {
                        "type_constraints": [42]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": 42, "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "", "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "   ", "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": 42, "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "  ", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Label"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Label", "property": 42}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Label", "property": ""}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "a type constraint", "label": "Label", "property": "   "}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_existence_constraints_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": 42
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [42]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": 42, "label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "", "label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "   ", "label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": 42, "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": "", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": "  ", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": "Label"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": "Label", "property": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": "Label", "property": ""}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "existence_constraints": [
                                            {"name": "a type constraint", "label": "Label", "property": "   "}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraints_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": 42
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [42]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": 42, "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "   ", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": 42, "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "   ", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": []}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": [42]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": [""]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": ["   "]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_options_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "unique_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": ["property"], options: 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_key_constraints_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": 42
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_key_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [42]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": 42, "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "   ", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": 42, "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "   ", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": []}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": [42]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": [""]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": ["   "]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_key_constraint_options_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "label": "Label", "properties": ["property"], options: 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_range_indexes_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": 42
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_range_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [42]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": 42, "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "   ", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": 42, "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "   ", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "Label"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "Label", "properties": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "Label", "properties": []}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "Label", "properties": [42]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "Label", "properties": [""]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "range_indexes": [
                                            {"name": "a range index", "label": "Label", "properties": ["   "]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_indexes_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": 42
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_text_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [42]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": 42, "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "", "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "   ", "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": 42, "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "   ", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Label"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Label", "property": 42}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Label", "property": ""}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Label", "property": "   "}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_index_options_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "text_indexes": [
                                    {"name": "a text index", "label": "Label", "property": "property", "options": 42}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_point_indexes_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": 42
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_point_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [42]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": 42, "label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "", "label": "Label", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "   ", "label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": 42, "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "", "properties": ["property"]}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "   ", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_point_index_properties_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "Label"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_point_index_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "Label", "property": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_point_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "Label", "property": ""}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_point_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "Label", "property": "   "}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_point_index_options_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "point_indexes": [
                                            {"name": "a point index", "label": "Label", "property": "property", "options": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_indexes_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": 42
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [42]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"labels": ["Label"], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": 42, "labels": ["Label"], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "", "labels": ["Label"], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "   ", "labels": ["Label"], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'labels' not found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": 42, "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": [42], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": [""], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["   "], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "active": true,
                    "name": "a-target",
                    "source": "a-source",
                    "write_mode": "merge",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "field", "target_property": "property"}
                    ],
                    "schema": {
                        "fulltext_indexes": [
                            {"name": "a fulltext index", "labels": ["Label", "Label", "Label"], "properties": ["property"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[DUPL-010][$.targets.nodes[0].schema.fulltext_indexes[0].label] $.targets.nodes[0].schema.fulltext_indexes[0].label \"Label\" must be defined at most once but 3 occurrences were found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_is_dangling_and_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label1", "Label1", "Label1"], "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "3 error(s)",
                        "0 warning(s)",
                        "[DANG-019][$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]] $.targets.nodes[0].schema.fulltext_indexes[0].labels[0] \"Label1\" is not part of the defined labels",
                        "[DANG-019][$.targets.nodes[0].schema.fulltext_indexes[0].labels[1]] $.targets.nodes[0].schema.fulltext_indexes[0].labels[1] \"Label1\" is not part of the defined labels",
                        "[DANG-019][$.targets.nodes[0].schema.fulltext_indexes[0].labels[2]] $.targets.nodes[0].schema.fulltext_indexes[0].labels[2] \"Label1\" is not part of the defined labels")
                .hasMessageNotContaining("[DUPL-010]");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label"], "properties": 42}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label"], "properties": [42]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label"], "properties": [""]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label"], "properties": ["   "]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_options_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": {
                                "fulltext_indexes": [
                                    {"name": "a fulltext index", "labels": ["Label"], "properties": ["property"], "options": 42}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_vector_indexes_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": 42
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_vector_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [42]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"label": "Label", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": 42, "label": "Label", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "", "label": "Label", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "   ", "label": "Label", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": 42, "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "   ", "property": "property", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "Label", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "Label", "property": 42, "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "Label", "property": "", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "Label", "property": "   ", "options": {}}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_vector_index_options_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "Label", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'options' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_options_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "active": true,
                                    "name": "a-target",
                                    "source": "a-source",
                                    "write_mode": "merge",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "field", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "a vector index", "label": "Label", "property": "property", "options": 42}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_key_and_existence_constraints_are_defined_on_same_labels_and_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property"]}
                                ],
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Label", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_are_defined_on_same_properties_but_different_labels() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property"]}
                                ],
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Label2", "property": "property"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void fails_if_node_key_constraint_overlap_with_node_existence_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "id", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1", "property2"]}
                                ],
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Label", "property": "property2"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_properties_overlap_but_reference_different_labels() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "active": true,
                    "name": "a-target",
                    "source": "a-source",
                    "write_mode": "merge",
                    "labels": ["Label1", "Label2"],
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "id", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "label": "Label1", "properties": ["property1", "property2"]}
                        ],
                        "existence_constraints": [
                            {"name": "an existence constraint", "label": "Label2", "property": "property2"}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_are_defined_on_same_label_but_different_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1"]}
                                ],
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Label", "property": "property2"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_and_existence_constraint_define_invalid_labels() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "not-a-label", "properties": ["property1"]}
                                ],
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "not-a-label", "property": "property1"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_key_and_existence_constraint_defines_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1"]}
                                ],
                                "existence_constraints": [
                                    {"name": "an existence constraint", "label": "Label", "property": "not-a-prop"}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_key_and_unique_constraints_are_defined_on_same_labels_and_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1", "property2"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["property1", "property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and unique constraints: unique_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_properties_but_different_labels() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label2", "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void does_not_fail_if_node_key_and_unique_constraints_are_defined_on_same_properties_in_different_order() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property1", "property2"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label1", "properties": ["property2", "property1"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_node_key_and_unique_constraints_only_share_property_subset() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property1", "property2"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label1", "properties": ["property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_label_but_different_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_and_unique_constraints_define_invalid_labels() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "not-a-label", "properties": ["property1"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "not-a-label", "properties": ["property1"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_node_key_and_unique_constraints_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["not-a-prop"]}
                                ],
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["not-a-prop"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_key_constraint_and_range_index_are_defined_on_same_labels_and_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1", "property2"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["property1", "property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key constraint and range index: range_indexes[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_properties_but_different_labels() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label2", "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void
            does_not_fail_if_node_key_constraint_and_range_index_are_defined_on_same_properties_in_different_order() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property1", "property2"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label1", "properties": ["property2", "property1"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_node_key_constraint_and_range_index_only_share_property_subset() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label1", "properties": ["property1", "property2"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label1", "properties": ["property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_label_but_different_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["property1"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_constraint_and_range_index_define_invalid_labels() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "not-a-label", "properties": ["property1"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "not-a-label", "properties": ["property1"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_node_key_constraint_and_range_index_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "a key constraint", "label": "Label", "properties": ["not-a-prop"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["not-a-prop"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_unique_constraint_and_range_index_are_defined_on_same_labels_and_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["property1", "property2"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["property1", "property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant unique constraint and range index: range_indexes[0], unique_constraints[0]");
    }

    @Test
    public void
            does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_properties_but_different_labels() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label1", "properties": ["property"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label2", "properties": ["property"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void
            does_not_fail_if_node_unique_constraint_and_range_index_are_defined_on_same_properties_in_different_order() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label1", "properties": ["property1", "property2"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label1", "properties": ["property2", "property1"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_node_unique_constraint_and_range_index_only_share_property_subset() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label1", "properties": ["property1", "property2"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label1", "properties": ["property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void
            does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_label_but_different_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["property1"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["property2"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_unique_constraint_and_range_index_define_invalid_labels() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "not-a-label", "properties": ["property1"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "not-a-label", "properties": ["property1"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_node_unique_constraint_and_range_index_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "active": true,
                            "name": "a-target",
                            "source": "a-source",
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "property1"},
                                {"source_field": "name", "target_property": "property2"}
                            ],
                            "schema": {
                                "unique_constraints": [
                                    {"name": "a unique constraint", "label": "Label", "properties": ["not-a-prop"]}
                                ],
                                "range_indexes": [
                                    {"name": "a range index", "label": "Label", "properties": ["not-a-prop"]}
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void deserializes_full_start_and_end_node_references() throws SpecificationException {
        var specification = deserialize(new StringReader("""
                                {
                                    "version": "1",
                                    "sources": [
                                         {
                                             "name": "movies-source",
                                             "type": "jdbc",
                                             "data_source": "db",
                                             "sql": "SELECT id, title FROM db.movies"
                                         },
                                         {
                                             "name": "categories-source",
                                             "type": "jdbc",
                                             "data_source": "db",
                                             "sql": "SELECT id, name FROM db.categories"
                                         },
                                         {
                                             "name": "movies-in-categories-source",
                                             "type": "jdbc",
                                             "data_source": "db",
                                             "sql": "SELECT movie_id, category_id FROM db.movies_in_categories"
                                         },
                                    ],
                                    "targets": {
                                        "nodes": [
                                             {
                                                 "name": "movie",
                                                 "source": "movies-source",
                                                 "labels": ["Movie"],
                                                 "properties": [
                                                     {"source_field": "id", "target_property": "identifier"},
                                                     {"source_field": "title", "target_property": "title"},
                                                 ],
                                                 "schema": {
                                                   "key_constraints": [
                                                       {"name": "movie-id-key-constraint", "label": "Movie", "properties": ["identifier"]},
                                                   ]
                                                 }
                                             },
                                             {
                                                 "name": "category",
                                                 "source": "categories-source",
                                                 "labels": ["Category"],
                                                 "properties": [
                                                     {"source_field": "id", "target_property": "identifier"},
                                                     {"source_field": "name", "target_property": "name"}
                                                 ],
                                                 "schema": {
                                                   "key_constraints": [
                                                       {"name": "category-id-key-constraint", "label": "Category", "properties": ["identifier"]}
                                                   ]
                                                 }
                                             }
                                        ],
                                        "relationships": [
                                             {
                                                 "name": "in-category",
                                                 "source": "movies-in-categories-source",
                                                 "type": "IN_CATEGORY",
                                                 "start_node_reference": {
                                                      "name": "movie",
                                                      "key_mappings": [
                                                         {
                                                             "source_field": "movie_id",
                                                             "node_property": "identifier"
                                                         }
                                                      ]
                                                 },
                                                 "end_node_reference": {
                                                      "name": "category",
                                                      "key_mappings": [
                                                         {
                                                             "source_field": "category_id",
                                                             "node_property": "identifier"
                                                         }
                                                      ]
                                                 }
                                             }
                                        ]
                                    }
                                }
                                """));

        var relationships = specification.getTargets().getRelationships();
        assertThat(relationships).hasSize(1);
        var relationship = relationships.getFirst();
        var startNode = relationship.getStartNodeReference();
        assertThat(startNode.getName()).isEqualTo("movie");
        var startKeyMappings = startNode.getKeyMappings();
        assertThat(startKeyMappings).hasSize(1);
        var startKeyMapping = startKeyMappings.getFirst();
        assertThat(startKeyMapping.getSourceField()).isEqualTo("movie_id");
        assertThat(startKeyMapping.getNodeProperty()).isEqualTo("identifier");
        var endNode = relationship.getEndNodeReference();
        assertThat(endNode.getName()).isEqualTo("category");
        assertThat(endNode.getKeyMappings()).hasSize(1);
        var endKeyMapping = endNode.getKeyMappings().getFirst();
        assertThat(endKeyMapping.getSourceField()).isEqualTo("category_id");
        assertThat(endKeyMapping.getNodeProperty()).isEqualTo("identifier");
    }

    @Test
    public void fails_if_relationship_target_active_attribute_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "active": 42,
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].active: integer found, boolean expected");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0]: required property 'source' not found");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "type": "TYPE",
                                    "source": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].source: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "type": "TYPE",
                                    "source": "",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].source: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "type": "TYPE",
                                    "source": "   ",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].source: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_write_mode_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].write_mode: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_write_mode_has_wrong_value() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "not-a-valid-mode",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].write_mode: does not have a value in the enumeration");
    }

    @Test
    public void fails_if_relationship_target_node_match_mode_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "node_match_mode": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].node_match_mode: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_node_match_mode_has_wrong_value() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "node_match_mode": "not-a-valid-mode",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].node_match_mode: does not have a value in the enumeration");
    }

    @Test
    public void fails_if_relationship_start_node_reference_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": 42,
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_start_node_reference_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "target_id"
                            }
                        ]
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_start_node_reference_name_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": 42,
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                        "node_property": "target_id"
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_start_node_reference_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "name": "",
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "target_id"
                            }
                        ]
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_start_node_reference_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "name": "    ",
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "target_id"
                            }
                        ]
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "name": "a-node-target"
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: required property 'key_mappings' not found");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": 42
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": []
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [42]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_source_field_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "node_property": "target_id"
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0]: required property 'source_field' not found");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_source_field_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": 30,
                                        "node_property": "target_id"
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].source_field: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_source_field_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "",
                                        "node_property": "target_id"
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].source_field: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_source_field_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "   ",
                                        "node_property": "target_id"
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].source_field: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_node_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0]: required property 'node_property' not found");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_node_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                        "node_property": 30,
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_node_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                        "node_property": ""
                                    }
                                ]
                            },
                            "end_node_reference": "a-node-target"
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_start_node_reference_key_mappings_node_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "   "
                            }
                        ]
                    },
                    "end_node_reference": "a-node-target"
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_end_node_reference_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "write_mode": "create",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": 42
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "target_id"
                            }
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_end_node_reference_name_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": 42,
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "target_id"
                            }
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": "",
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "target_id"
                            }
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_end_node_reference_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "    ",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                        "node_property": "target_id"
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": "a-node-target"
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: required property 'key_mappings' not found");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": 42
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": []
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [42]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_source_field_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "node_property": "target_id"
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0]: required property 'source_field' not found");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_source_field_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": 30,
                                        "node_property": "target_id"
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].source_field: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_source_field_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "",
                                        "node_property": "target_id"
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].source_field: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_source_field_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": [
                            {
                                "source_field": "   ",
                                "node_property": "target_id"
                            }
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].source_field: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_target_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0]: required property 'node_property' not found");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_node_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                        "node_property": 30,
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_node_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                {
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
                        "type": "jdbc",
                        "data_source": "a-data-source",
                        "sql": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": [{
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "id", "target_property": "id"}
                            ]
                        }],
                        "relationships": [{
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": {
                                "name": "a-node-target",
                                "key_mappings": [
                                    {
                                        "source_field": "source_id",
                                        "node_property": ""
                                    }
                                ]
                            }
                        }]
                    }
                }
                """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_end_node_reference_key_mappings_node_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-node-target",
                    "source": "a-source",
                    "labels": ["Label"],
                    "properties": [
                        {"source_field": "id", "target_property": "id"}
                    ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "write_mode": "create",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": {
                        "name": "a-node-target",
                        "key_mappings": [
                            {
                                "source_field": "source_id",
                                "node_property": "   "
                            }
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_type_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships[0]: required property 'type' not found");
    }

    @Test
    public void fails_if_relationship_target_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].type: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_type_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].type: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_type_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label"],
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "   ",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].type: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": 42
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                42
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"target_property": "id"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0]: required property 'source_field' not found");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": 42, "target_property": "id"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "", "target_property": "id"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "   ", "target_property": "id"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0]: required property 'target_property' not found");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": 42}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": ""}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": "   "}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": "id", "target_property_type": 42}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property_type: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_type_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                          "version": "1",
                          "sources": [{
                            "name": "a-source",
                            "type": "jdbc",
                            "data_source": "a-data-source",
                            "sql": "SELECT id, name FROM my.table"
                          }],
                          "targets": {
                            "nodes": [{
                              "name": "a-node-target",
                              "source": "a-source",
                              "labels": ["Label"],
                              "properties": [
                                {"source_field": "id", "target_property": "id"}
                              ]
                            }],
                            "relationships": [{
                              "name": "a-relationship-target",
                              "source": "a-source",
                              "type": "TYPE",
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "properties": [
                                {"source_field": "id", "target_property": "id", "target_property_type": "blackhole"}
                              ]
                            }]
                          }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property_type: does not have a value in the enumeration");
    }

    @Test
    public void fails_if_relationship_schema_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": 42
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraints_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [{
                                        "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [{
                                        "name": 42, "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [{
                                        "name": "", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [{
                                        "name": "    ", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [
                                        {"name": "a type constraint"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [
                                        {"name": "a type constraint", "property": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [
                                        {"name": "a type constraint", "property": ""}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "type_constraints": [
                                        {"name": "a type constraint", "property": "    "}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_key_constraints_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [{
                                        "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [{
                                        "name": 42, "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [{
                                        "name": "", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [{
                                        "name": "    ", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": [42]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": []}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": [""]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": ["    "]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_options_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": ["id"], "options": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraints_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [{
                                        "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [{
                                        "name": 42, "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [{
                                        "name": "", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [{
                                        "name": "    ", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": [42]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": []}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_element_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": [""]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_element_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": ["    "]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_options_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": ["id"], "options": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraints_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraints_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [{
                                        "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [{
                                        "name": 42, "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [{
                                        "name": "", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [{
                                        "name": "    ", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [
                                        {"name": "an existence constraint"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [
                                        {"name": "an existence constraint", "property": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [
                                        {"name": "an existence constraint", "property": ""}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_existence_constraint_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "existence_constraints": [
                                        {"name": "an existence constraint", "property": "    "}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_range_indexes_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_range_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_range_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [{
                                        "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_range_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [{
                                        "name": 42, "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_range_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [{
                                        "name": "", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_range_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [{
                                        "name": "    ", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_range_index_properties_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [
                                        {"name": "a range index"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_relationship_schema_range_index_properties_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [
                                        {"name": "a range index", "properties": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_range_index_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [
                                        {"name": "a range index", "properties": [42]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_range_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [
                                        {"name": "a range index", "properties": [""]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_range_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "range_indexes": [
                                        {"name": "a range index", "properties": ["    "]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_text_indexes_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_text_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_text_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [{
                                        "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_text_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [{
                                        "name": 42, "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_text_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [{
                                        "name": "", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_text_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [{
                                        "name": "    ", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_text_index_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [
                                        {"name": "a text index"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_relationship_schema_text_index_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [
                                        {"name": "a text index", "property": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_text_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [
                                        {"name": "a text index", "property": ""}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_text_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [
                                        {"name": "a text index", "property": "    "}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_text_index_options_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "text_indexes": [
                                        {"name": "a text index", "property": "id", "options": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_point_indexes_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_point_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_point_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [{
                                        "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_point_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [{
                                        "name": 42, "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_point_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [{
                                        "name": "", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_point_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [{
                                        "name": "    ", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_point_index_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [
                                        {"name": "a point index"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_relationship_schema_point_index_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [
                                        {"name": "a point index", "property": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_point_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [
                                        {"name": "a point index", "property": ""}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_point_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [
                                        {"name": "a point index", "property": "    "}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_point_index_options_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "point_indexes": [
                                        {"name": "a point index", "property": "id", "options": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_indexes_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [{
                                        "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [{
                                        "name": 42, "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [{
                                        "name": "", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [{
                                        "name": "    ", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_properties_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [
                                        {"name": "a fulltext index"}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_properties_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [
                                        {"name": "a fulltext index", "properties": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_properties_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [
                                        {"name": "a fulltext index", "properties": [42]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [
                                        {"name": "a fulltext index", "properties": [""]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [
                                        {"name": "a fulltext index", "properties": ["    "]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_fulltext_index_options_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "fulltext_indexes": [
                                        {"name": "a fulltext index", "properties": ["id"], "options": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_vector_indexes_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": 42
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_vector_indexes_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [42]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [{
                                        "property": "id", "options": {}
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_name_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [{
                                        name: 42, "property": "id", "options": {}
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [{
                                        "name": "", "property": "id", "options": {}
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [{
                                        "name": "    ", "property": "id", "options": {}
                                    }]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_property_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [
                                        {"name": "a vector index", "options": {}}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_property_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [
                                        {"name": "a vector index", "property": 42, "options": {}}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_property_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [
                                        {"name": "a vector index", "property": "", "options": {}}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_property_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [
                                        {"name": "a vector index", "property": "    ", "options": {}}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_vector_index_options_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                  "name": "a-relationship-target",
                                  "source": "a-source",
                                  "type": "TYPE",
                                  "start_node_reference": "a-node-target",
                                  "end_node_reference": "a-node-target",
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                    "vector_indexes": [
                                        {"name": "a vector index", "property": "id", "options": 42}
                                    ]
                                  }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_key_and_existence_constraints_are_defined_on_same_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "properties": ["property"]}
                                        ],
                                        "existence_constraints": [
                                            {"name": "an existence constraint", "property": "property"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @Test
    public void fails_if_relationship_key_constraint_overlap_with_relationship_existence_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "id", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                                        ],
                                        "existence_constraints": [
                                            {"name": "an existence constraint", "property": "property2"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_are_not_defined_on_same_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ],
                                  "schema": {
                                      "key_constraints": [{
                                         "name": "a-key-constraint",
                                         "label": "Label",
                                         "properties": ["id"]
                                      }]
                                  }
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "properties": ["property1"]}
                                        ],
                                        "existence_constraints": [
                                            {"name": "an existence constraint", "property": "property2"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_and_existence_constraints_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "data_source": "a-data-source",
                                "sql": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "nodes": [{
                                  "name": "a-node-target",
                                  "source": "a-source",
                                  "labels": ["Label"],
                                  "properties": [
                                    {"source_field": "id", "target_property": "id"}
                                  ]
                                }],
                                "relationships": [{
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "property1"},
                                        {"source_field": "name", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "key_constraints": [
                                            {"name": "a key constraint", "properties": ["not-a-prop"]}
                                        ],
                                        "existence_constraints": [
                                            {"name": "an existence constraint", "property": "not-a-prop"}
                                        ]
                                    }
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.existence_constraints[0].property \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_key_and_unique_constraints_are_defined_on_same_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                        ],
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property1", "property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key and unique constraints: unique_constraints[0], key_constraints[0]");
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void
            does_not_fail_if_relationship_key_and_unique_constraints_are_defined_on_same_properties_in_different_order() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                        ],
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property2", "property1"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_key_and_unique_constraints_only_share_property_subset() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                        ],
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_and_unique_constraints_are_not_defined_on_same_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1"]}
                        ],
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_relationship_key_and_unique_constraints_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["not-a-prop"]}
                        ],
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["not-a-prop"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_key_constraint_and_range_index_are_defined_on_same_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property1", "property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant key constraint and range index: range_indexes[0], key_constraints[0]");
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void
            does_not_fail_if_relationship_key_constraint_and_range_index_are_defined_on_same_properties_in_different_order() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property2", "property1"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_key_constraint_and_range_index_only_share_property_subset() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1", "property2"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_constraint_and_range_index_are_not_defined_on_same_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["property1"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_relationship_key_constraint_and_range_index_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "key_constraints": [
                            {"name": "a key constraint", "properties": ["not-a-prop"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["not-a-prop"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_unique_constraint_and_range_index_are_defined_on_same_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property1", "property2"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property1", "property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema defines redundant unique constraint and range index: range_indexes[0], unique_constraints[0]");
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void
            does_not_fail_if_relationship_unique_constraint_and_range_index_are_defined_on_same_properties_in_different_order() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property1", "property2"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property2", "property1"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_unique_constraint_and_range_index_only_share_property_subset() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property1", "property2"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_unique_constraint_and_range_index_are_not_defined_on_same_properties() {
        assertThatCode(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["property1"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["property2"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent()))).doesNotThrowAnyException();
    }

    @Test
    public void
            does_not_report_redundancy_if_relationship_unique_constraint_and_range_index_define_invalid_properties() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "properties": [
                        {"source_field": "id", "target_property": "property1"},
                        {"source_field": "name", "target_property": "property2"}
                    ],
                    "schema": {
                        "unique_constraints": [
                            {"name": "a unique constraint", "properties": ["not-a-prop"]}
                        ],
                        "range_indexes": [
                            {"name": "a range index", "properties": ["not-a-prop"]}
                        ]
                    }
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void reports_redundancy_for_start_node_reference_also_declared_as_explicit_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }, {
                  "name": "another-node-target",
                  "source": "a-source",
                  "labels": ["Label2"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "another-key-constraint",
                         "label": "Label2",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "another-node-target",
                    "dependencies": ["a-node-target"]
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].dependencies \"a-node-target\" is defined as an explicit dependency *and* as a start node reference, remove it from dependencies");
    }

    @Test
    public void reports_redundancy_for_end_node_reference_also_declared_as_explicit_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }, {
                  "name": "another-node-target",
                  "source": "a-source",
                  "labels": ["Label2"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "another-key-constraint",
                         "label": "Label2",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "another-node-target",
                    "dependencies": ["another-node-target"]
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].dependencies \"another-node-target\" is defined as an explicit dependency *and* as an end node reference, remove it from dependencies");
    }

    @Test
    public void
            does_not_report_redundancy_for_node_reference_also_declared_as_explicit_dependency_if_a_node_reference_is_dangling() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ]
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "not-a-node-target",
                    "dependencies": ["a-node-target"]
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].end_node_reference] $.targets.relationships[0].end_node_reference refers to a non-existing node target \"not-a-node-target\"");
    }

    @Test
    public void
            does_not_report_redundancy_for_node_reference_also_declared_as_explicit_dependency_if_a_dependency_is_dangling() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "dependencies": ["a-node-target", "another-node-target"]
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0]] $.targets.relationships[0] depends on a non-existing target \"another-node-target\"");
    }

    @Test
    public void
            does_not_report_redundancy_for_node_reference_also_declared_as_explicit_dependency_if_a_dependency_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "a-data-source",
                "sql": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                  "name": "a-node-target",
                  "source": "a-source",
                  "labels": ["Label"],
                  "properties": [
                    {"source_field": "id", "target_property": "id"}
                  ],
                  "schema": {
                      "key_constraints": [{
                         "name": "a-key-constraint",
                         "label": "Label",
                         "properties": ["id"]
                      }]
                  }
                }],
                "relationships": [{
                    "name": "a-relationship-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "a-node-target",
                    "end_node_reference": "a-node-target",
                    "dependencies": ["a-node-target", "a-node-target"]
                }]
            }
        }
        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].depends_on] $.targets.relationships[0].depends_on defines dependency \"a-node-target\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_source_is_missing_type() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0]: required property 'type' not found");
    }

    @Test
    void fails_if_source_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": 42,
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0].type: integer found, string expected");
    }

    @Test
    void fails_if_source_type_is_not_supported_by_any_loaded_source_provider() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "unsupported",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(UndeserializableSourceException.class)
                .hasMessageContainingAll(
                        "Expected exactly one source provider for sources of type unsupported, but found: 0");
    }

    @Test
    void fails_if_third_party_source_and_supplier_do_not_match() {
        assertThatThrownBy(() -> deserialize(new StringReader("""
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "jdbc",
                                "sql": "SELECT p.productname FROM products p ORDER BY p.productname ASC "
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            }
                        }
                        """.stripIndent())))
                .isInstanceOf(UndeserializableSourceException.class)
                .hasMessageContainingAll(
                        "Source provider org.neo4j.importer.v1.sources.JdbcSourceProvider failed to deserialize the following source definition");
    }
}
