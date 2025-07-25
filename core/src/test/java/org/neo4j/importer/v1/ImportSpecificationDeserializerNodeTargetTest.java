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
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.WriteMode;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;

public class ImportSpecificationDeserializerNodeTargetTest {

    private static final String ENUM_ARRAY_PROPERTY_TYPE =
            ImportSpecificationDeserializerEnumUtil.enumToJsonString(PropertyType.class);

    private static final String ENUM_ARRAY_WRITE_MODE =
            ImportSpecificationDeserializerEnumUtil.enumToJsonString(WriteMode.class);

    @Test
    public void fails_if_node_target_active_attribute_has_wrong_type() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].active: integer found, boolean expected");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_missing() {
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
                            "labels": ["Label"],
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
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'source' not found");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_wrongly_typed() {
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
                            "source": 42,
                            "labels": ["Label"],
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
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].source: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_empty() {
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
                            "source": "",
                            "labels": ["Label"],
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
                        "0 warning(s)", "$.targets.nodes[0].source: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_source_attribute_is_blank() {
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
                            "source": "   ",
                            "labels": ["Label"],
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
                        "$.targets.nodes[0].source: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_write_mode_has_wrong_type() {
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
                                    "write_mode": 42,
                                    "labels": ["Label"],
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
                        "0 warning(s)", "$.targets.nodes[0].write_mode: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_write_mode_has_wrong_value() {
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
                                    "write_mode": "reticulating_splines",
                                    "labels": ["Label"],
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
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].write_mode: does not have a value in the enumeration",
                        ENUM_ARRAY_WRITE_MODE);
    }

    @Test
    public void fails_if_node_target_labels_is_missing() {
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
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'labels' not found");
    }

    @Test
    public void fails_if_node_target_labels_is_wrongly_typed() {
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
                                    "labels": 42,
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
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].labels: integer found, array expected");
    }

    @Test
    public void fails_if_node_target_labels_is_empty() {
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
                                    "labels": [],
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
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_target_labels_element_is_wrongly_typed() {
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
                                    "labels": [42],
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
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].labels[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_labels_element_is_empty() {
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
                                    "labels": [""],
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
                        "0 warning(s)", "$.targets.nodes[0].labels[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_labels_element_is_blank() {
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
                                    "labels": ["   "],
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
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].labels[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_property_mappings_is_missing() {
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
                                    "labels": ["Label"]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_target_property_mappings_is_wrongly_typed() {
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
                                    "properties": 42
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_element_is_wrongly_typed() {
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
                                    "properties": [42]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_missing() {
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
                                        {"target_property": "property"}
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
                        "$.targets.nodes[0].properties[0]: required property 'source_field' not found");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_wrongly_typed() {
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
                                        {"source_field": 42, "target_property": "property"}
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
                        "$.targets.nodes[0].properties[0].source_field: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_empty() {
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
                                        {"source_field": "", "target_property": "property"}
                                    ]
                                }]
                            }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].source_field: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_property_mappings_source_field_is_blank() {
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
                                        {"source_field": "   ", "target_property": "property"}
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
                        "$.targets.nodes[0].properties[0].source_field: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_missing() {
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
                        "$.targets.nodes[0].properties[0]: required property 'target_property' not found");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_wrongly_typed() {
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
                        "$.targets.nodes[0].properties[0].target_property: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_empty() {
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
                        "$.targets.nodes[0].properties[0].target_property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_is_blank() {
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
                        "$.targets.nodes[0].properties[0].target_property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_type_is_wrongly_typed() {
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
                                {"source_field": "id", "target_property": "property", "target_property_type": 42}
                            ]
                        }]
                        }
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property_type: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_property_mappings_target_property_type_is_unsupported() {
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
                                {"source_field": "id", "target_property": "property", "target_property_type": "blackhole"}
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
                        "$.targets.nodes[0].properties[0].target_property_type: does not have a value in the enumeration",
                        ENUM_ARRAY_PROPERTY_TYPE);
    }

    @Test
    public void fails_if_node_schema_is_wrongly_typed() {
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
                            "write_mode": "merge",
                            "labels": ["Label"],
                            "properties": [
                                {"source_field": "field", "target_property": "property"}
                            ],
                            "schema": 42
                        }]
                    }
                }
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.targets.nodes[0].schema: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_type_constraints_are_wrongly_typed() {
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
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_type_constraints_element_is_wrongly_typed() {
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
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_type_constraint_name_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_type_constraint_label_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_type_constraint_property_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.type_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_existence_constraints_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraints_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_name_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_label_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_existence_constraint_property_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraints_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraints_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_name_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_label_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_element_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_properties_element_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_unique_constraint_options_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_key_constraints_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_key_constraints_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_key_constraint_name_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_key_constraint_label_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_element_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_key_constraint_properties_element_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_key_constraint_options_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_range_indexes_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_range_indexes_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_range_index_name_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_range_index_label_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties: must have at least 1 items but found 0");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_element_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_range_index_properties_element_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_indexes_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_text_indexes_element_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_text_index_name_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_text_index_label_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_text_index_property_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_text_index_options_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.text_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_point_indexes_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_point_indexes_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_point_index_name_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_point_index_label_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_point_index_properties_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_point_index_property_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_point_index_property_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_point_index_property_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_point_index_options_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.point_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_indexes_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_indexes_element_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_name_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_are_missing() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'labels' not found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_are_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_element_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_element_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_element_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].labels[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_is_duplicated() {
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
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[DUPL-010][$.targets.nodes[0].schema.fulltext_indexes[0].label] $.targets.nodes[0].schema.fulltext_indexes[0].label \"Label\" must be defined at most once but 3 occurrences were found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_labels_is_dangling_and_duplicated() {
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
                """
                                .stripIndent())))
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0]: required property 'properties' not found");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_are_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_element_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_element_is_empty() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_properties_element_is_blank() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].properties[0]: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_fulltext_index_options_is_wrongly_typed() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.fulltext_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_vector_indexes_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes: integer found, array expected");
    }

    @Test
    public void fails_if_node_schema_vector_indexes_element_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'name' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_vector_index_name_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'label' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_vector_index_label_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].label: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'property' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: integer found, string expected");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_empty() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_schema_vector_index_property_is_blank() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].property: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_schema_vector_index_options_is_missing() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0]: required property 'options' not found");
    }

    @Test
    public void fails_if_node_schema_vector_index_options_is_wrongly_typed() {
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
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.vector_indexes[0].options: integer found, object expected");
    }

    @Test
    public void fails_if_key_and_existence_constraints_are_defined_on_same_labels_and_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_are_defined_on_same_properties_but_different_labels() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void fails_if_key_constraint_overlap_with_existence_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and existence constraints: existence_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_properties_overlap_but_reference_different_labels() {
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
        """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_and_existence_constraints_are_defined_on_same_label_but_different_properties() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_and_existence_constraint_define_invalid_labels() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_key_and_existence_constraint_defines_invalid_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.existence_constraints[0].property \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_key_and_unique_constraints_are_defined_on_same_labels_and_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key and unique constraints: unique_constraints[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_properties_but_different_labels() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_properties_in_different_order() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_and_unique_constraints_only_share_property_subset() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_and_unique_constraints_are_defined_on_same_label_but_different_properties() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_and_unique_constraints_define_invalid_labels() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_key_and_unique_constraints_define_invalid_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_key_constraint_and_range_index_are_defined_on_same_labels_and_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant key constraint and range index: range_indexes[0], key_constraints[0]");
    }

    @Test
    public void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_properties_but_different_labels() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_properties_in_different_order() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_constraint_and_range_index_only_share_property_subset() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_key_constraint_and_range_index_are_defined_on_same_label_but_different_properties() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_key_constraint_and_range_index_define_invalid_labels() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_key_constraint_and_range_index_define_invalid_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.key_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }

    @Test
    public void fails_if_unique_constraint_and_range_index_are_defined_on_same_labels_and_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema defines redundant unique constraint and range index: range_indexes[0], unique_constraints[0]");
    }

    @Test
    public void
            does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_properties_but_different_labels() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    // https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/using-indexes/#composite-indexes-property-order
    public void does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_properties_in_different_order() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_fail_if_unique_constraint_and_range_index_only_share_property_subset() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void
            does_not_fail_if_unique_constraint_and_range_index_are_defined_on_same_label_but_different_properties() {
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
                """
                                .stripIndent())))
                .doesNotThrowAnyException();
    }

    @Test
    public void does_not_report_redundancy_if_unique_constraint_and_range_index_define_invalid_labels() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].label \"not-a-label\" is not part of the defined labels",
                        "$.targets.nodes[0].schema.range_indexes[0].label \"not-a-label\" is not part of the defined labels");
    }

    @Test
    public void does_not_report_redundancy_if_unique_constraint_and_range_index_define_invalid_properties() {
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
                """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].schema.unique_constraints[0].properties[0] \"not-a-prop\" is not part of the property mappings",
                        "$.targets.nodes[0].schema.range_indexes[0].properties[0] \"not-a-prop\" is not part of the property mappings");
    }
}
