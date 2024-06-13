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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import org.junit.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;

public class ImportSpecificationDeserializerRelationshipTargetTest {

    @Test
    public void fails_if_relationship_target_active_attribute_has_wrong_type() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].active: integer found, boolean expected");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_missing() {
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
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
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
                        "$.targets.relationships[0]: required property 'source' not found");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].source: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].source: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_source_attribute_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].source: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_write_mode_has_wrong_type() {
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
                                    "write_mode": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].write_mode: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_write_mode_has_wrong_value() {
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
                                    "write_mode": "not-a-valid-mode",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
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
                        "$.targets.relationships[0].write_mode: does not have a value in the enumeration [create, merge]");
    }

    @Test
    public void fails_if_relationship_target_node_match_mode_has_wrong_type() {
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
                                    "node_match_mode": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].node_match_mode: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_node_match_mode_has_wrong_value() {
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
                                    "node_match_mode": "not-a-valid-mode",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
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
                        "$.targets.relationships[0].node_match_mode: does not have a value in the enumeration [match, merge]");
    }

    @Test
    public void fails_if_relationship_start_node_reference_has_wrong_type() {
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
                                    "start_node_reference": 42,
                                    "end_node_reference": "a-node-target"
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].start_node_reference: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_end_node_reference_has_wrong_type() {
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
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": 42
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].end_node_reference: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_type_is_missing() {
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
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.relationships[0]: required property 'type' not found");
    }

    @Test
    public void fails_if_relationship_target_type_is_wrongly_typed() {
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
                                    "type": 42,
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
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
                        "$.targets.relationships[0].type: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_type_is_empty() {
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
                                    "type": "",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
                                    ]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)", "$.targets.relationships[0].type: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_type_is_blank() {
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
                                    "type": "   ",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "a-node-target",
                                    "properties": [
                                        {"source_field": "id", "target_property": "id"}
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
                        "$.targets.relationships[0].type: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_is_wrongly_typed() {
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
                              "properties": 42
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_element_is_wrongly_typed() {
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
                                42
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
                        "$.targets.relationships[0].properties[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_missing() {
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
                                {"target_property": "id"}
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
                        "$.targets.relationships[0].properties[0]: required property 'source_field' not found");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_wrongly_typed() {
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
                                {"source_field": 42, "target_property": "id"}
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
                        "$.targets.relationships[0].properties[0].source_field: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_empty() {
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
                                {"source_field": "", "target_property": "id"}
                              ]
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].source_field: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_source_field_is_blank() {
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
                                {"source_field": "   ", "target_property": "id"}
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
                        "$.targets.relationships[0].properties[0].source_field: does not match the regex pattern \\S+");
    }

    @Test
    public void does_not_fail_if_relationship_target_property_mappings_source_field_is_listed_in_target_aggregations() {
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
                              "start_node_reference": "a-node-target",
                              "end_node_reference": "a-node-target",
                              "source_transformations": {
                                "aggregations": [{
                                    "field_name": "aggregated", "expression": "42"
                                }]
                              },
                              "properties": [
                                {"source_field": "aggregated", "target_property": "id"}
                              ]
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_missing() {
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
                                {"source_field": "id"}
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
                        "$.targets.relationships[0].properties[0]: required property 'target_property' not found");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_wrongly_typed() {
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
                                {"source_field": "id", "target_property": 42}
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
                        "$.targets.relationships[0].properties[0].target_property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_empty() {
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
                                {"source_field": "id", "target_property": ""}
                              ]
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_is_blank() {
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
                                {"source_field": "id", "target_property": "   "}
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
                        "$.targets.relationships[0].properties[0].target_property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_type_is_wrongly_typed() {
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
                                {"source_field": "id", "target_property": "id", "target_property_type": 42}
                              ]
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].properties[0].target_property_type: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_property_mappings_target_property_type_is_unsupported() {
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
                                {"source_field": "id", "target_property": "id", "target_property_type": "blackhole"}
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
                        "$.targets.relationships[0].properties[0].target_property_type: does not have a value in the enumeration [boolean, boolean_array, byte_array, date, date_array, duration, duration_array, float, float_array, integer, integer_array, local_datetime, local_datetime_array, local_time, local_time_array, point, point_array, string, string_array, zoned_datetime, zoned_datetime_array, zoned_time, zoned_time_array]");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_enable_grouping_has_wrong_type() {
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
                              "source_transformations": {
                                "enable_grouping": 42
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
                        "$.targets.relationships[0].source_transformations.enable_grouping: integer found, boolean expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_have_wrong_type() {
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
                              "source_transformations": {
                                  "aggregations": 42
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
                        "$.targets.relationships[0].source_transformations.aggregations: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_element_has_wrong_type() {
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
                              "source_transformations": {
                                "aggregations": [42]
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
                        "$.targets.relationships[0].source_transformations.aggregations[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_expression_is_missing() {
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
                              "source_transformations": {
                                "aggregations": [{
                                    "field_name": "field_2"
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
                        "$.targets.relationships[0].source_transformations.aggregations[0]: required property 'expression' not found");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_expression_has_wrong_type() {
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
                              "source_transformations": {
                                "aggregations": [{
                                    "expression": 42,
                                    "field_name": "field_2"
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
                        "$.targets.relationships[0].source_transformations.aggregations[0].expression: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_expression_is_empty() {
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
                              "source_transformations": {
                                  "aggregations": [{
                                      "expression": "",
                                      "field_name": "field_2"
                                  }]
                              }
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].source_transformations.aggregations[0].expression: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_expression_is_blank() {
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
                              "source_transformations": {
                                  "aggregations": [{
                                      "expression": "  ",
                                      "field_name": "field_2"
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
                        "$.targets.relationships[0].source_transformations.aggregations[0].expression: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_field_name_is_missing() {
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
                              "source_transformations": {
                                "aggregations": [{
                                    "expression": "42"
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
                        "$.targets.relationships[0].source_transformations.aggregations[0]: required property 'field_name' not found");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_field_name_has_wrong_type() {
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
                              "source_transformations": {
                                "aggregations": [{
                                    "expression": "42",
                                    "field_name": 42
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
                        "$.targets.relationships[0].source_transformations.aggregations[0].field_name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_field_name_is_empty() {
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
                              "source_transformations": {
                                 "aggregations": [{
                                     "expression": "42",
                                     "field_name": ""
                                 }]
                              }
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].source_transformations.aggregations[0].field_name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_aggregations_field_name_is_blank() {
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
                              "source_transformations": {
                                  "aggregations": [{
                                      "expression": "42",
                                      "field_name": " "
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
                        "$.targets.relationships[0].source_transformations.aggregations[0].field_name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_where_clause_has_wrong_type() {
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
                              "source_transformations": {
                                  "where": 42
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
                        "$.targets.relationships[0].source_transformations.where: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_where_clause_is_empty() {
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
                              "source_transformations": {
                                  "where": ""
                              }
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].source_transformations.where: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_where_clause_is_blank() {
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
                              "source_transformations": {
                                  "where": "  "
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
                        "$.targets.relationships[0].source_transformations.where: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_is_wrongly_typed() {
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
                              "source_transformations": {
                                "order_by": 42
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
                        "$.targets.relationships[0].source_transformations.order_by: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_element_is_wrongly_typed() {
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
                              "source_transformations": {
                                  "order_by": [42]
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
                        "$.targets.relationships[0].source_transformations.order_by[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_expression_is_missing() {
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
                              "source_transformations": {
                                 "order_by": [{}]
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
                        "$.targets.relationships[0].source_transformations.order_by[0]: required property 'expression' not found");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_expression_is_wrongly_typed() {
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
                              "source_transformations": {
                                 "order_by": [{
                                     "expression": 42
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
                        "$.targets.relationships[0].source_transformations.order_by[0].expression: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_expression_is_empty() {
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
                              "source_transformations": {
                                  "order_by": [{
                                      "expression": ""
                                  }]
                              }
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].source_transformations.order_by[0].expression: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_expression_is_blank() {
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
                              "source_transformations": {
                                  "order_by": [{
                                      "expression": "   "
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
                        "$.targets.relationships[0].source_transformations.order_by[0].expression: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_order_is_wrongly_typed() {
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
                              "source_transformations": {
                                  "order_by": [{
                                      "expression": "42",
                                      "order": 42
                                  }]
                              }
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].source_transformations.order_by[0].order: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_order_by_order_has_wrong_value() {
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
                              "source_transformations": {
                                  "order_by": [{
                                      "expression": "42",
                                      "order": "new"
                                  }]
                              }
                            }]
                          }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].source_transformations.order_by[0].order: does not have a value in the enumeration [ASC, DESC]");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_limit_is_wrongly_typed() {
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
                              "source_transformations": {
                                "limit": []
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
                        "$.targets.relationships[0].source_transformations.limit: array found, integer expected");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_limit_is_negative() {
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
                              "source_transformations": {
                                  "limit": -42
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
                        "$.targets.relationships[0].source_transformations.limit: must have a minimum value of 1");
    }

    @Test
    public void fails_if_relationship_target_source_transformation_limit_is_zero() {
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
                              "source_transformations": {
                                  "limit": 0
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
                        "$.targets.relationships[0].source_transformations.limit: must have a minimum value of 1");
    }

    @Test
    public void fails_if_relationship_schema_is_wrongly_typed() {
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
                                  "schema": 42
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.relationships[0].schema: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraints_are_wrongly_typed() {
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
                                    "type_constraints": 42
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
                        "$.targets.relationships[0].schema.type_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraints_element_is_wrongly_typed() {
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
                                    "type_constraints": [42]
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
                        "$.targets.relationships[0].schema.type_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_missing() {
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
                                    "type_constraints": [{
                                        "property": "id"
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
                        "$.targets.relationships[0].schema.type_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_wrongly_typed() {
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
                                    "type_constraints": [{
                                        "name": 42, "property": "id"
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
                        "$.targets.relationships[0].schema.type_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_empty() {
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
                                    "type_constraints": [{
                                        "name": "", "property": "id"
                                    }]
                                  }
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_name_is_blank() {
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
                                    "type_constraints": [{
                                        "name": "    ", "property": "id"
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
                        "$.targets.relationships[0].schema.type_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_missing() {
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
                                    "type_constraints": [
                                        {"name": "a type constraint"}
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
                        "$.targets.relationships[0].schema.type_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_wrongly_typed() {
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
                                    "type_constraints": [
                                        {"name": "a type constraint", "property": 42}
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
                        "$.targets.relationships[0].schema.type_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_empty() {
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
                                    "type_constraints": [
                                        {"name": "a type constraint", "property": ""}
                                    ]
                                  }
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.type_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_type_constraint_property_is_blank() {
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
                                    "type_constraints": [
                                        {"name": "a type constraint", "property": "    "}
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
                        "$.targets.relationships[0].schema.type_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_key_constraints_are_wrongly_typed() {
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
                                    "key_constraints": 42
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
                        "$.targets.relationships[0].schema.key_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraints_element_is_wrongly_typed() {
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
                                    "key_constraints": [42]
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
                        "$.targets.relationships[0].schema.key_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_missing() {
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
                                        "properties": ["id"]
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
                        "$.targets.relationships[0].schema.key_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_wrongly_typed() {
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
                                        "name": 42, "properties": ["id"]
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
                        "$.targets.relationships[0].schema.key_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_empty() {
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
                                        "name": "", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_name_is_blank() {
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
                                        "name": "    ", "properties": ["id"]
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
                        "$.targets.relationships[0].schema.key_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_are_missing() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint"}
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
                        "$.targets.relationships[0].schema.key_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_are_wrongly_typed() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": 42}
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
                        "$.targets.relationships[0].schema.key_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_element_is_wrongly_typed() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": [42]}
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
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_are_empty() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": []}
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
                        "$.targets.relationships[0].schema.key_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_element_is_empty() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": [""]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_properties_element_is_blank() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": ["    "]}
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
                        "$.targets.relationships[0].schema.key_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_key_constraint_options_are_wrongly_typed() {
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
                                    "key_constraints": [
                                        {"name": "a key constraint", "properties": ["id"], "options": 42}
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
                        "$.targets.relationships[0].schema.key_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraints_are_wrongly_typed() {
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
                                    "unique_constraints": 42
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
                        "$.targets.relationships[0].schema.unique_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraints_element_is_wrongly_typed() {
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
                                    "unique_constraints": [42]
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
                        "$.targets.relationships[0].schema.unique_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_missing() {
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
                                        "properties": ["id"]
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
                        "$.targets.relationships[0].schema.unique_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_wrongly_typed() {
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
                                        "name": 42, "properties": ["id"]
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
                        "$.targets.relationships[0].schema.unique_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_empty() {
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
                                        "name": "", "properties": ["id"]
                                    }]
                                  }
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_name_is_blank() {
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
                                        "name": "    ", "properties": ["id"]
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
                        "$.targets.relationships[0].schema.unique_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_are_missing() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint"}
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
                        "$.targets.relationships[0].schema.unique_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_are_wrongly_typed() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": 42}
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
                        "$.targets.relationships[0].schema.unique_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_element_is_wrongly_typed() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": [42]}
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
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_are_empty() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": []}
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
                        "$.targets.relationships[0].schema.unique_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_element_is_empty() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": [""]}
                                    ]
                                  }
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_properties_element_is_blank() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": ["    "]}
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
                        "$.targets.relationships[0].schema.unique_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_relationship_schema_unique_constraint_options_are_wrongly_typed() {
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
                                    "unique_constraints": [
                                        {"name": "a unique constraint", "properties": ["id"], "options": 42}
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
                        "$.targets.relationships[0].schema.unique_constraints[0].options: integer found, object expected");
    }
}
