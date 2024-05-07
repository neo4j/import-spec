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

public class ImportSpecificationDeserializerNodeTargetTest {

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
                        "$.targets.nodes[0].write_mode: does not have a value in the enumeration [create, merge]");
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
    public void fails_if_node_target_property_mappings_source_field_is_not_listed_in_text_source_fields() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["field-1"],
        "data": [
            ["foo"], ["bar"]
        ]
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "labels": ["Label"],
            "properties": [
                {"source_field": "invalid-field", "target_property": "property"}
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
                        "$.targets.nodes[0].properties[0].source_field field \"invalid-field\" is neither defined in the target's text source nor its source transformations");
    }

    @Test
    public void does_not_fail_if_node_target_property_mappings_source_field_is_listed_in_target_aggregations() {
        assertThatCode(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["field-1"],
        "data": [
            ["foo"], ["bar"]
        ]
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "labels": ["Label"],
            "source_transformations": {
                "aggregations": [{
                    "field_name": "aggregated", "expression": "42"
                }]
            },
            "properties": [
                {"source_field": "aggregated", "target_property": "property"}
            ]
        }]
    }
}
"""
                                .stripIndent())))
                .doesNotThrowAnyException();
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
}
"""
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.targets.nodes[0].properties[0].target_property \"property\" must be defined only once but found 2 occurrences");
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
                        "$.targets.nodes[0].properties[0].target_property_type: does not have a value in the enumeration [boolean, boolean_array, byte_array, date, date_array, duration, duration_array, float, float_array, integer, integer_array, local_datetime, local_datetime_array, local_time, local_time_array, point, point_array, string, string_array, zoned_datetime, zoned_datetime_array, zoned_time, zoned_time_array]");
    }

    @Test
    public void fails_if_node_target_source_transformation_enable_grouping_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.enable_grouping: integer found, boolean expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_have_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations: integer found, array expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_element_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_expression_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0]: required property 'expression' not found");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_expression_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].expression: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_expression_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].expression: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_expression_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].expression: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_field_name_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0]: required property 'field_name' not found");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_field_name_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].field_name: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_field_name_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].field_name: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_field_name_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].field_name: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_field_names_clash() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["field_1"],
        "data": [
            ["foo"], ["bar"]
        ]
    }],
    "targets": {
        "nodes": [{
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property1"},
                {"source_field": "field_2", "target_property": "property2"}
            ],
            "source_transformations": {
                "aggregations": [{
                    "expression": "42",
                    "field_name": "field_2"
                },{
                    "expression": "42",
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].field_name \"field_2\" must be defined only once but is currently defined 2 times within this target's aggregations");
    }

    @Test
    public void fails_if_node_target_source_transformation_aggregations_field_name_clashes_with_text_source_fields() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["field_1"],
        "data": [
            ["foo"], ["bar"]
        ]
    }],
    "targets": {
        "nodes": [{
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
            ],
            "source_transformations": {
                "aggregations": [{
                    "expression": "42",
                    "field_name": "field_1"
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
                        "$.targets.nodes[0].source_transformations.aggregations[0].field_name \"field_1\" is already defined in the target's source header");
    }

    @Test
    public void fails_if_node_target_source_transformation_where_clause_has_wrong_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.where: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_where_clause_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.where: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_source_transformation_where_clause_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.where: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by: integer found, array expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_element_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0]: integer found, object expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_expression_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0]: required property 'expression' not found");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_expression_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0].expression: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_expression_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0].expression: must be at least 1 characters long");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_expression_is_blank() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0].expression: does not match the regex pattern \\S+");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_order_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0].order: integer found, string expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_order_by_order_has_wrong_value() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.order_by[0].order: does not have a value in the enumeration [ASC, DESC]");
    }

    @Test
    public void fails_if_node_target_source_transformation_limit_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field_1", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.limit: array found, integer expected");
    }

    @Test
    public void fails_if_node_target_source_transformation_limit_is_negative() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.limit: must have a minimum value of 1");
    }

    @Test
    public void fails_if_node_target_source_transformation_limit_is_zero() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "version": "1",
    "sources": [{
        "name": "a-source",
        "type": "jdbc",
        "data_source": "a-data-source",
        "sql": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "nodes": [{
            "active": true,
            "name": "a-target",
            "source": "a-source",
            "write_mode": "merge",
            "labels": ["Label"],
            "properties": [
                {"source_field": "field", "target_property": "property"}
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
                        "$.targets.nodes[0].source_transformations.limit: must have a minimum value of 1");
    }
}
