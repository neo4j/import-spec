/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

class ImportSpecificationDeserializerTest {

    @Test
    void fails_if_spec_is_unparseable() {
        assertThatThrownBy(() -> deserialize(new StringReader("not-json")))
                .isInstanceOf(UnparseableSpecificationException.class);
    }

    @Test
    void fails_if_spec_is_not_deserializable() {
        assertThatThrownBy(() -> deserialize(new StringReader("[]")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: array found, object expected");
    }

    @Test
    void fails_if_sources_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "targets": {
                        "queries": [{
                            "name": "my-query",
                            "source": "my-bigquery-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'sources' not found");
    }

    @Test
    void fails_if_sources_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": 42,
                    "targets": {
                        "queries": [{
                            "name": "my-query",
                            "source": "my-bigquery-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources: integer found, array expected");
    }

    @Test
    void fails_if_source_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [42],
                    "targets": {
                        "queries": [{
                            "name": "my-query",
                            "source": "my-bigquery-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0]: integer found, object expected");
    }

    @Test
    void fails_if_sources_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [],
                    "targets": {
                        "queries": [{
                            "name": "my-query",
                            "source": "my-bigquery-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources: must have at least 1 items but found 0");
    }

    @Test
    void fails_if_targets_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "my-bigquery-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }]
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'targets' not found");
    }

    @Test
    void fails_if_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "my-bigquery-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": 42
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.targets: integer found, object expected");
    }

    @Test
    void fails_if_targets_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "my-bigquery-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {}
                }
                """
                                .stripIndent())))
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
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "my-bigquery-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "nodes": 42
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes: integer found, array expected");
    }

    @Test
    void fails_if_relationship_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "my-bigquery-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "relationships": 42
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships: integer found, array expected");
    }

    @Test
    void fails_if_custom_query_targets_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "my-bigquery-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": 42
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.queries: integer found, array expected");
    }

    @Test
    void fails_if_actions_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-minimal-custom-query-target",
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    },
                    "actions": 42
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions: integer found, array expected");
    }

    @Test
    void fails_if_action_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-minimal-custom-query-target",
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    },
                    "actions": [42]
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions[0]: integer found, object expected");
    }

    @Test
    void fails_if_name_is_missing_in_source() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-minimal-custom-query-target",
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_node_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_relationship_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "relationships": [{
                            "source": "a-source",
                            "type": "TYPE",
                            "write_mode": "create",
                            "node_match_mode": "create",
                            "start_node": {
                                "label": "Label1",
                                "key_properties": [
                                    {"source_field": "field_1", "target_property": "property1"}
                                ]
                            },
                            "end_node": {
                                "label": "Label2",
                                "key_properties": [
                                    {"source_field": "field_2", "target_property": "property2"}
                                ]
                            }
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.queries[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_missing_in_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-minimal-custom-query-target",
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    },
                    "actions": [{
                        "type": "http",
                        "method": "get",
                        "url": "https://example.com"
                    }]
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0]: required property 'name' not found");
    }

    @Test
    void fails_if_name_is_blank_in_source() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "name": "  ",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-minimal-custom-query-target",
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_node_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_relationship_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "relationships": [{
                            "name": "   ",
                            "source": "a-source",
                            "write_mode": "create",
                            "node_match_mode": "create",
                            "type": "TYPE",
                            "start_node": {
                                "label": "Label1",
                                "key_properties": [
                                    {"source_field": "field_1", "target_property": "property1"}
                                ]
                            },
                            "end_node": {
                                "label": "Label2",
                                "key_properties": [
                                    {"source_field": "field_2", "target_property": "property2"}
                                ]
                            }
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_name_is_blank_in_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "my-bigquery-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-minimal-custom-query-target",
                            "source": "a-source",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    },
                    "actions": [{
                        "name": "   ",
                        "type": "http",
                        "method": "get",
                        "url": "https://example.com"
                    }]
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].name: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_node_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.targets.nodes[0].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_rel_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "duplicate",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "relationships": [{
                            "name": "duplicate",
                            "source": "duplicate",
                            "type": "TYPE",
                            "start_node": {
                                "label": "Label1",
                                "key_properties": [
                                    {"source_field": "field_1", "target_property": "property1"}
                                ]
                            },
                            "end_node": {
                                "label": "Label2",
                                "key_properties": [
                                    {"source_field": "field_2", "target_property": "property2"}
                                ]
                            }
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.targets.relationships[0].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "duplicate",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "duplicate",
                            "source": "duplicate",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.targets.queries[0].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "duplicate",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "queries": [{
                            "name": "my-target",
                            "source": "duplicate",
                            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                        }]
                    },
                    "actions": [{
                        "name": "duplicate",
                        "type": "http",
                        "method": "get",
                        "url": "https://example.com"
                    }]
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.actions[0].name");
    }

    @Test
    void fails_if_node_target_does_not_refer_to_existing_source() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
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
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @Test
    void fails_if_relationship_target_does_not_refer_to_existing_source() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "type": "bigquery",
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "relationships": [{
                    "name": "a-target",
                    "source": "incorrect-source-name",
                    "type": "TYPE",
                    "start_node": {
                        "label": "Label1",
                        "key_properties": [
                            {"source_field": "field_1", "target_property": "property1"}
                        ]
                    },
                    "end_node": {
                        "label": "Label2",
                        "key_properties": [
                            {"source_field": "field_2", "target_property": "property2"}
                        ]
                    }
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @Test
    void fails_if_custom_query_target_does_not_refer_to_existing_source() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
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
"""
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0] refers to the non-existing source \"incorrect-source-name\". "
                                + "Possible names are: \"a-source\"");
    }

    @Test
    void fails_if_node_target_depends_on_non_existing_target_or_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "type": "bigquery",
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "nodes": [{
                    "name": "a-target",
                    "source": "a-source",
                    "depends_on": "incorrect-dependent",
                    "labels": ["Label1", "Label2"],
                    "write_mode": "create",
                    "properties": [
                        {"source_field": "field_1", "target_property": "property1"},
                        {"source_field": "field_2", "target_property": "property2"}
                    ]
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] depends on a non-existing action or target \"incorrect-dependent\"");
    }

    @Test
    void fails_if_relationship_target_depends_on_non_existing_target_or_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "type": "bigquery",
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "relationships": [{
                    "name": "a-target",
                    "source": "a-source",
                    "depends_on": "incorrect-dependent",
                    "type": "TYPE",
                    "start_node": {
                        "label": "Label1",
                        "key_properties": [
                            {"source_field": "field_1", "target_property": "property1"}
                        ]
                    },
                    "end_node": {
                        "label": "Label2",
                        "key_properties": [
                            {"source_field": "field_2", "target_property": "property2"}
                        ]
                    }
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0] depends on a non-existing action or target \"incorrect-dependent\"");
    }

    @Test
    void fails_if_custom_query_target_depends_on_non_existing_target_or_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "type": "bigquery",
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "depends_on": "incorrect-dependent",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0] depends on a non-existing action or target \"incorrect-dependent\"");
    }

    @Test
    void fails_if_action_depends_on_non_existing_target_or_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
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
                "name": "an-action",
                "depends_on": "incorrect-dependent",
                "type": "http",
                "method": "get",
                "url": "https://example.com"
            }]
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.actions[0] depends on a non-existing action or target \"incorrect-dependent\".");
    }

    @Test
    void fails_if_relationship_refers_to_a_non_existing_node_target_for_start() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "type": "bigquery",
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "relationships": [{
                    "name": "a-target",
                    "source": "a-source",
                    "type": "TYPE",
                    "start_node_reference": "incorrect-reference",
                    "end_node": {
                        "label": "Label2",
                        "key_properties": [
                            {"source_field": "field_2", "target_property": "property2"}
                        ]
                    }
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @Test
    void fails_if_relationship_refers_to_a_non_existing_node_target_for_end() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
                    "sources": [{
                        "type": "bigquery",
                        "name": "a-source",
                        "query": "SELECT id, name FROM my.table"
                    }],
                    "targets": {
                        "relationships": [{
                            "name": "a-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node": {
                                "label": "Label1",
                                "key_properties": [
                                    {"source_field": "field_1", "target_property": "property1"}
                                ]
                            },
                            "end_node_reference": "incorrect-reference"
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }
}
