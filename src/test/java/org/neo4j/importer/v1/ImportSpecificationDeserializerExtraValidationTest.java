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

// This exercises the compliance of various import spec payloads with the built-in validation plugins
// All payloads here 100% comply to the JSON schema
public class ImportSpecificationDeserializerExtraValidationTest {

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
                                "stage": "pre_relationships",
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
    void fails_if_node_target_depends_on_non_existing_target() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0] depends on a non-existing target \"invalid\"");
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
                                    "depends_on": ["invalid"],
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
                        "$.targets.relationships[0] depends on a non-existing target \"invalid\"");
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
                                    "depends_on": ["invalid"],
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
                        "$.targets.queries[0] depends on a non-existing target \"invalid\"");
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

    @Test
    void fails_if_direct_dependency_cycle_is_detected() {
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
                                    "depends_on": ["a-target"],
                                    "source": "a-source",
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
                        "1 error(s)", "0 warning(s)", "A dependency cycle has been detected: a-target->a-target");
    }

    @Test
    void fails_if_longer_dependency_cycle_is_detected() {
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
                                    "name": "a-relationship-target",
                                    "depends_on": ["a-query-target"],
                                    "source": "a-source",
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "A dependency cycle has been detected: a-relationship-target->a-query-target->another-query-target->a-relationship-target");
    }

    @Test
    void fails_if_dependency_cycle_is_detected_via_start_node_reference() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
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
                            "end_node": {
                                "label": "Label2",
                                "key_properties": [
                                    {"source_field": "description", "target_property": "property2"}
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
                        "A dependency cycle has been detected: a-node-target->a-relationship-target->a-node-target");
    }

    @Test
    void fails_if_dependency_cycle_is_detected_via_end_node_reference() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
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
                            "start_node": {
                                "label": "Label1",
                                "key_properties": [
                                    {"source_field": "description", "target_property": "property1"}
                                ]
                            },
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
                        "A dependency cycle has been detected: a-node-target->a-relationship-target->a-node-target");
    }

    @Test
    void does_not_report_cycles_if_names_are_duplicated() {
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
                                    "depends_on": ["a-target"],
                                    "source": "a-source",
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
                                }],
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "depends_on": ["a-target"],
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
                        "Name \"a-target\" is duplicated across the following paths: $.targets.relationships[0].name, $.targets.queries[0].name");
    }

    @Test
    void does_not_report_cycles_if_depends_on_are_dangling() {
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
                                    "name": "a-relationship-target",
                                    "depends_on": ["a-query-target"],
                                    "source": "a-source",
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[1] depends on a non-existing target \"invalid\"");
    }

    @Test
    void does_not_report_cycles_if_node_references_are_dangling() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
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
                            "start_node": {
                                "label": "Label1",
                                "key_properties": [
                                    {"source_field": "description", "target_property": "property1"}
                                ]
                            },
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[1].start_node_reference refers to a non-existing node target \"invalid-ref\"");
    }

    @Test
    void fails_if_external_text_source_header_includes_duplicated_names() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "sources": [{
                                "name": "a-source",
                                "type": "text",
                                "header": ["duplicate", "duplicate"],
                                "urls": [
                                    "https://example.com/my.csv"
                                ]
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
                        "1 error(s)",
                        "0 warning(s)",
                        "$.sources[0].header defines column \"duplicate\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_inline_text_source_header_includes_duplicated_names() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "sources": [{
                                "name": "a-source",
                                "type": "text",
                                "header": ["duplicate", "duplicate"],
                                "data": [
                                    ["foo", "bar"], ["bar", "qix"]
                                ]
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
                        "1 error(s)",
                        "0 warning(s)",
                        "$.sources[0].header defines column \"duplicate\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_inline_text_source_line_data_rows_have_fewer_entries_than_header_column_count() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                                  "value1"
                                ]
                              ]
                            }
                          ],
                          "targets": {
                            "queries": [
                              {
                                "name": "a-target",
                                "source": "a-source",
                                "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }
                            ]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.sources[0].data[0]] row defines 1 column(s), expected at least 2");
    }

    @Test
    void fails_if_all_targets_are_inactive() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                            "queries": [
                              {
                                "active": false,
                                "name": "a-target",
                                "source": "a-source",
                                "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }
                            ]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "[$.targets] at least one target must be active, none found");
    }

    @Test
    void fails_if_node_target_duplicates_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                                    {"source_field": "field_1", "target_property": "property1"},
                                    {"source_field": "field_2", "target_property": "property2"}
                                ]
                            }],
                            "queries": [
                              {
                                "name": "a-query-target",
                                "source": "a-source",
                                "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }
                            ]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].depends_on defines dependency \"a-query-target\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_relationship_target_duplicates_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                            "relationships": [{
                                "name": "a-relationship-target",
                                "source": "a-source",
                                "type": "TYPE",
                                "depends_on": ["a-query-target", "a-query-target"],
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
                            }],
                            "queries": [
                              {
                                "name": "a-query-target",
                                "source": "a-source",
                                "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }
                            ]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].depends_on defines dependency \"a-query-target\" 2 times, it must be defined at most once");
    }

    @Test
    void fails_if_query_target_duplicates_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                            "queries": [
                              {
                                "name": "a-query-target",
                                "source": "a-source",
                                "depends_on": ["a-relationship-target", "a-relationship-target"],
                                "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }
                            ],
                            "relationships": [{
                                "name": "a-relationship-target",
                                "source": "a-source",
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
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @Test
    void does_not_report_if_targets_duplicate_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                            "queries": [
                              {
                                "name": "a-query-target",
                                "source": "a-source",
                                "depends_on": ["a-relationship-target", "a-relationship-target"],
                                "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                              }
                            ],
                            "relationships": [{
                                "name": "a-relationship-target",
                                "source": "a-source",
                                "type": "TYPE",
                                "depends_on": ["a-query-target"],
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
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }
}
