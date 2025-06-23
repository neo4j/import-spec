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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;

// This exercises the compliance of various import spec payloads with the built-in validation plugins
// All payloads here 100% comply to the JSON schema
public class ImportSpecificationDeserializerExtraValidationTest {

    @Test
    void fails_if_source_name_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.sources[1].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_target_name() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.targets.nodes[0].name");
    }

    @Test
    void fails_if_source_name_is_duplicated_with_action_name() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.sources[0].name, $.actions[0].name");
    }

    @Test
    void fails_if_node_target_name_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.nodes[1].name");
    }

    @Test
    void fails_if_node_target_name_is_duplicated_with_rel_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.relationships[0].name");
    }

    @Test
    void fails_if_node_target_name_is_duplicated_with_custom_query_target() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.nodes[0].name, $.targets.queries[0].name");
    }

    @Test
    void fails_if_query_target_name_is_duplicated_with_action() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "Name \"duplicate\" is duplicated across the following paths: $.targets.queries[0].name, $.actions[0].name");
    }

    @Test
    void fails_if_node_target_does_not_refer_to_existing_source() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference refers to a non-existing node target \"incorrect-reference\".");
    }

    @Test
    void fails_if_active_relationship_target_refers_to_an_inactive_node_target_for_start() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference belongs to an active target but refers to an inactive node target \"a-node-target\"");
    }

    @Test
    void fails_if_active_relationship_target_refers_to_an_inactive_node_target_for_end() {
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
                                            "name": "a-key-constraint",
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference belongs to an active target but refers to an inactive node target \"another-node-target\"");
    }

    @Test
    void does_not_fail_if_inactive_relationship_refers_to_an_inactive_node_target_for_start() {
        assertThatNoException()
                .isThrownBy(() -> deserialize(new StringReader(
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
                                                   "name": "a-key-constraint",
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
                                """
                                .stripIndent())));
    }

    @Test
    void does_not_fail_if_inactive_relationship_refers_to_an_inactive_node_target_for_end() {
        assertThatNoException()
                .isThrownBy(() -> deserialize(new StringReader(
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
                                """
                                .stripIndent())));
    }

    @Test
    void does_not_report_inactive_node_reference_when_other_references_are_dangling() {
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
                                           "name": "a-key-constraint",
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[1].start_node_reference refers to a non-existing node target \"invalid-ref\"");
    }

    @Test
    void fails_if_all_targets_are_inactive() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @Test
    void does_not_report_cycles_if_targets_duplicate_dependency() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.queries[0].depends_on defines dependency \"a-relationship-target\" 2 times, it must be defined at most once");
    }

    @Test
    public void fails_if_node_target_labels_are_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels[0] \"Label1\" must be defined only once but found 2 occurrences",
                        "$.targets.nodes[0].labels[2] \"Label2\" must be defined only once but found 3 occurrences");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                          {
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
                        }"""
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_duplicated() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
    }

    @Test
    public void fails_if_node_target_type_constraint_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_type_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_type_constraint_property_refers_to_an_untyped_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property \"untyped\" refers to an untyped property");
    }

    @Test
    public void fails_if_relationship_target_type_constraint_property_refers_to_an_untyped_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """

                                {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property \"untyped\" refers to an untyped property");
    }

    @Test
    public void fails_if_node_target_unique_constraint_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_unique_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_key_constraint_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_key_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_existence_constraint_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_existence_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_range_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_range_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_text_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_text_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_point_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_point_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_fulltext_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0] \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_fulltext_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_node_target_vector_index_refers_to_non_existent_label() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label \"Invalid\" is not part of the defined labels");
    }

    @Test
    public void fails_if_node_target_vector_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_type_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_key_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_unique_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_existence_constraint_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.existence_constraints[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_range_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.range_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_text_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.text_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_point_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.point_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_fulltext_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.fulltext_indexes[0].properties[0] \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_target_vector_index_property_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.vector_indexes[0].property \"invalid\" is not part of the property mappings");
    }

    @Test
    public void fails_if_relationship_start_node_reference_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference.key_mappings[0].node_property refers to a non-existing node property \"not-a-valid-property\"");
    }

    @Test
    public void fails_if_relationship_end_node_reference_refers_to_non_existent_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference.key_mappings[0].node_property refers to a non-existing node property \"not-a-valid-property\"");
    }

    @Test
    public void fails_if_relationship_start_node_reference_refers_to_non_key_and_non_unique_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.relationships[0].start_node_reference.key_mappings[0].node_property] Property 'prop' is not part of start node target's a-node-target key and unique properties");
    }

    @Test
    public void fails_if_relationship_end_node_reference_refers_to_non_key_and_non_unique_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.relationships[0].end_node_reference.key_mappings[0].node_property] Property 'prop' is not part of end node target's a-node-target key and unique properties");
    }

    @Test
    public void does_not_fail_if_relationship_start_node_reference_refers_to_node_key_property() {
        assertThatCode(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_end_node_reference_refers_to_node_key_property() {
        assertThatCode(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_start_node_reference_refers_to_node_unique_property() {
        assertThatCode(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_relationship_end_node_reference_refers_to_node_unique_property() {
        assertThatCode(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void fails_if_relationship_start_node_reference_does_not_refer_to_node_key_or_unique_property() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "[$.targets.relationships[0].start_node_reference] Node a-node-target must define a key or unique constraint for property id, none found");
    }

    @Test
    public void fails_if_relationship_end_node_reference_refers_to_node_without_keys_nor_unique_properties() {
        assertThatCode(() -> deserialize(new StringReader(
                        """
                        {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].start_node_reference] Node a-node-target must define a key or unique constraint for property id, none found",
                        "[$.targets.relationships[0].end_node_reference] Node a-node-target must define a key or unique constraint for property id, none found");
    }
}
