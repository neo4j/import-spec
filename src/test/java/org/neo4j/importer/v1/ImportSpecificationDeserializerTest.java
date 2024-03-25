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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.UnparseableSpecificationException;

// This exercises the compliance of various import spec payloads with the JSON schema
// The class focuses on the general structure of the spec.
class ImportSpecificationDeserializerTest {

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
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$: required property 'version' not found");
    }

    @Test
    void fails_if_version_is_invalid() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.version: must be the constant value '1'");
    }

    @Test
    void fails_if_sources_are_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
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
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
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
                    "version": "1",
                    "sources": [{
                        "name": "a-source",
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
                            "node_match_mode": "create",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "a-node-target"
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
                            "node_match_mode": "create",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "a-node-target"
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].name: does not match the regex pattern \\S+");
    }
}
