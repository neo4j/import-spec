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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.actions.ActionStage;
import org.neo4j.importer.v1.actions.plugin.CypherAction;
import org.neo4j.importer.v1.actions.plugin.CypherExecutionMode;
import org.neo4j.importer.v1.sources.BigQuerySource;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.Targets;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

// This exercises the compliance of various import spec payloads with the JSON schema
// The class focuses on the general structure of the spec.
class ImportSpecificationDeserializerTest {

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
    void fails_if_action_is_wrongly_typed() {
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
}
