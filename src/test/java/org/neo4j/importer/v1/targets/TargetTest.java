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
package org.neo4j.importer.v1.targets;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TargetTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();

    @Test
    void deserializes_minimal_custom_query_target() throws Exception {
        var json =
                """
        {
            "name": "my-minimal-custom-query-target",
            "source": "a-source",
            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
        }
        """
                        .stripIndent();

        var target = mapper.readValue(json, CustomQueryTarget.class);

        assertThat(target.getName()).isEqualTo("my-minimal-custom-query-target");
        assertThat(target.isActive()).isTrue();
        assertThat(target.getSource()).isEqualTo("a-source");
        assertThat(target.getQuery()).isEqualTo("UNWIND $rows AS row CREATE (n:ANode) SET n = row");
    }

    @Test
    void deserializes_custom_query_target() throws Exception {
        var json =
                """
        {
            "name": "my-custom-query-target",
            "active": false,
            "source": "a-source",
            "depends_on": ["another-action-or-target"],
            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
        }
        """
                        .stripIndent();

        var target = mapper.readValue(json, CustomQueryTarget.class);

        assertThat(target.getName()).isEqualTo("my-custom-query-target");
        assertThat(target.isActive()).isFalse();
        assertThat(target.getSource()).isEqualTo("a-source");
        assertThat(target.getDependencies()).isEqualTo(List.of("another-action-or-target"));
        assertThat(target.getQuery()).isEqualTo("UNWIND $rows AS row CREATE (n:ANode) SET n = row");
    }

    @Test
    void deserializes_minimal_node_target() throws Exception {
        var json =
                """
        {
            "name": "my-minimal-node-target",
            "source": "a-source",
            "labels": ["Label1", "Label2"],
            "properties": [
                {"source_field": "field_1", "target_property": "property1"},
                {"source_field": "field_2", "target_property": "property2"}
            ]
        }
        """
                        .stripIndent();

        var target = mapper.readValue(json, NodeTarget.class);

        assertThat(target.getName()).isEqualTo("my-minimal-node-target");
        assertThat(target.isActive()).isTrue();
        assertThat(target.getSource()).isEqualTo("a-source");
        assertThat(target.getLabels()).isEqualTo(List.of("Label1", "Label2"));
        assertThat(target.getProperties())
                .isEqualTo(List.of(
                        new PropertyMapping("field_1", "property1", null),
                        new PropertyMapping("field_2", "property2", null)));
    }

    @Test
    void deserializes_node_target() throws Exception {
        var json =
                """
        {
            "name": "my-node-target",
            "active": false,
            "source": "a-source",
            "depends_on": ["an-action-or-target"],
            "write_mode": "MERGE",
            "source_transformations": {
                "enable_grouping": true,
                "aggregations": [
                    {"expression": "SUM(unit_price*quantity)", "field_name": "total_price"},
                    {"expression": "SUM(quantity)", "field_name": "total_quantity"}
                ],
                "where": "column IS NOT NULL",
                "order_by": [
                    {"expression": "column_1"},
                    {"expression": "column_2", "order": "ASC"},
                    {"expression": "column_3", "order": "DESC"}
                ],
                "limit": 42
            },
            "labels": ["Label1", "Label2"],
            "properties": [
                {"source_field": "field_1", "target_property": "property1", "target_property_type": "integer"},
                {"source_field": "field_2", "target_property": "property2", "target_property_type": "ZONED_DATETIME_ARRAY"},
                {"source_field": "field_3", "target_property": "property3", "target_property_type": "boolean"}
            ],
            "schema": {
                "enable_type_constraints": true,
                "key_constraints": [
                    {"name": "key_constraint_1", "label": "Label1", "properties": ["property1"]},
                    {"name": "key_constraint_2", "label": "Label2", "properties": ["property2"], "options": {"indexProvider": "range-1.0"}}
                ],
                "unique_constraints": [
                    {"name": "unique_constraint_1", "label": "Label1", "properties": ["property3"], "options": {"indexProvider": "range-1.0"}}
                ],
                "existence_constraints": [
                    {"name": "existence_constraint_1", "label": "Label2", "property": "property3"}
                ],
                "range_indexes": [
                    {"name": "range_index_1", "label": "Label1", "properties": ["property1", "property3"]},
                    {"name": "range_index_2", "label": "Label2", "properties": ["property2"]}
                ],
                "text_indexes": [
                    {"name": "text_index_1", "label": "Label1", "property": "property1", "options": {"indexProvider": "text-2.0"}},
                    {"name": "text_index_2", "label": "Label2", "property": "property2"}
                ],
                "point_indexes": [
                    {"name": "point_index_1", "label": "Label1", "property": "property1", "options": {"indexConfig": {"spatial.cartesian.min": [-100.0, -100.0], "spatial.cartesian.max": [100.0, 100.0]}}},
                    {"name": "point_index_2", "label": "Label2", "property": "property2"}
                ],
                "fulltext_indexes": [
                    {"name": "fulltext_index_1", "labels": ["Label1", "Label2"], "properties": ["property1"], "options": {"indexConfig": {"fulltext.analyzer": "english"}}},
                    {"name": "fulltext_index_2", "labels": ["Label1", "Label2"], "properties": ["property2", "property3"]}
                ],
                "vector_indexes": [
                    {"name": "vector_index_1", "label": "Label1", "property": "property1", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                ]
            }
        }
        """
                        .stripIndent();

        var target = mapper.readValue(json, NodeTarget.class);

        assertThat(target.getName()).isEqualTo("my-node-target");
        assertThat(target.isActive()).isFalse();
        assertThat(target.getSource()).isEqualTo("a-source");
        assertThat(target.getDependencies()).isEqualTo(List.of("an-action-or-target"));
        assertThat(target.getWriteMode()).isEqualTo(WriteMode.MERGE);
        assertThat(target.getSourceTransformations())
                .isEqualTo(new SourceTransformations(
                        true,
                        List.of(
                                new Aggregation("SUM(unit_price*quantity)", "total_price"),
                                new Aggregation("SUM(quantity)", "total_quantity")),
                        "column IS NOT NULL",
                        List.of(
                                new OrderBy("column_1", null),
                                new OrderBy("column_2", Order.ASC),
                                new OrderBy("column_3", Order.DESC)),
                        42));
        assertThat(target.getLabels()).isEqualTo(List.of("Label1", "Label2"));
        assertThat(target.getProperties())
                .isEqualTo(List.of(
                        new PropertyMapping("field_1", "property1", PropertyType.INTEGER),
                        new PropertyMapping("field_2", "property2", PropertyType.ZONED_DATETIME_ARRAY),
                        new PropertyMapping("field_3", "property3", PropertyType.BOOLEAN)));
        assertThat(target.getSchema())
                .isEqualTo(new NodeSchema(
                        true,
                        List.of(
                                new NodeKeyConstraint("key_constraint_1", "Label1", List.of("property1"), null),
                                new NodeKeyConstraint(
                                        "key_constraint_2",
                                        "Label2",
                                        List.of("property2"),
                                        Map.of("indexProvider", "range-1.0"))),
                        List.of(new NodeUniqueConstraint(
                                "unique_constraint_1",
                                "Label1",
                                List.of("property3"),
                                Map.of("indexProvider", "range-1.0"))),
                        List.of(new NodeExistenceConstraint("existence_constraint_1", "Label2", "property3")),
                        List.of(
                                new NodeRangeIndex("range_index_1", "Label1", List.of("property1", "property3")),
                                new NodeRangeIndex("range_index_2", "Label2", List.of("property2"))),
                        List.of(
                                new NodeTextIndex(
                                        "text_index_1", "Label1", "property1", Map.of("indexProvider", "text-2.0")),
                                new NodeTextIndex("text_index_2", "Label2", "property2", null)),
                        List.of(
                                new NodePointIndex(
                                        "point_index_1",
                                        "Label1",
                                        "property1",
                                        Map.of(
                                                "indexConfig",
                                                Map.of(
                                                        "spatial.cartesian.min",
                                                        List.of(-100.0, -100.0),
                                                        "spatial.cartesian.max",
                                                        List.of(100.0, 100.0)))),
                                new NodePointIndex("point_index_2", "Label2", "property2", null)),
                        List.of(
                                new NodeFullTextIndex(
                                        "fulltext_index_1",
                                        List.of("Label1", "Label2"),
                                        List.of("property1"),
                                        Map.of("indexConfig", Map.of("fulltext.analyzer", "english"))),
                                new NodeFullTextIndex(
                                        "fulltext_index_2",
                                        List.of("Label1", "Label2"),
                                        List.of("property2", "property3"),
                                        null)),
                        List.of(new NodeVectorIndex(
                                "vector_index_1",
                                "Label1",
                                "property1",
                                Map.of("vector.dimensions", 1536, "vector.similarity_function", "cosine")))));
    }

    @Test
    void deserializes_minimal_relationship_target() throws Exception {
        var json =
                """
        {
            "name": "my-minimal-relationship-target",
            "source": "a-source",
            "type": "TYPE",
            "start_node_reference": "a-node-target",
            "end_node_reference": "another-node-target"
        }
        """
                        .stripIndent();

        var target = mapper.readValue(json, RelationshipTarget.class);

        assertThat(target.getStartNodeReference()).isEqualTo("a-node-target");
        assertThat(target.getEndNodeReference()).isEqualTo("another-node-target");
    }

    @Test
    void deserializes_relationship_target() throws Exception {
        var json =
                """
                {
            "name": "my-relationship-target",
            "active": false,
            "source": "a-source",
            "depends_on": ["an-action-or-target"],
            "write_mode": "MERGE",
            "node_match_mode": "MATCH",
            "source_transformations": {
                "enable_grouping": true,
                "aggregations": [
                    {"expression": "SUM(unit_price*quantity)", "field_name": "total_price"},
                    {"expression": "SUM(quantity)", "field_name": "total_quantity"}
                ],
                "where": "column IS NOT NULL",
                "order_by": [
                    {"expression": "column_1"},
                    {"expression": "column_2", "order": "ASC"},
                    {"expression": "column_3", "order": "DESC"}
                ],
                "limit": 42
            },
            "type": "TYPE",
            "start_node_reference": "a-node-target",
            "end_node_reference": "another-node-target",
            "properties": [
                {"source_field": "field_1", "target_property": "property1", "target_property_type": "LOCAL_TIME_ARRAY"},
                {"source_field": "field_2", "target_property": "property2", "target_property_type": "STRING_ARRAY"},
                {"source_field": "field_3", "target_property": "property3", "target_property_type": "FLOAT"}
            ],
            "schema": {
                "enable_type_constraints": true,
                "key_constraints": [
                    {"name": "key_constraint_1", "properties": ["property1"]},
                    {"name": "key_constraint_2", "properties": ["property2"], "options": {"indexProvider": "range-1.0"}}
                ],
                "unique_constraints": [
                    {"name": "unique_constraint_1", "properties": ["property3"], "options": {"indexProvider": "range-1.0"}}
                ],
                "existence_constraints": [
                    {"name": "existence_constraint_1", "property": "property3"}
                ],
                "range_indexes": [
                    {"name": "range_index_1", "properties": ["property1", "property3"]},
                    {"name": "range_index_2", "properties": ["property2"]}
                ],
                "text_indexes": [
                    {"name": "text_index_1", "property": "property1", "options": {"indexProvider": "text-2.0"}},
                    {"name": "text_index_2", "property": "property2"}
                ],
                "point_indexes": [
                    {"name": "point_index_1", "property": "property1", "options": {"indexConfig": {"spatial.cartesian.min": [-100.0, -100.0], "spatial.cartesian.max": [100.0, 100.0]}}},
                    {"name": "point_index_2", "property": "property2"}
                ],
                "fulltext_indexes": [
                    {"name": "fulltext_index_1", "properties": ["property1"], "options": {"indexConfig": {"fulltext.analyzer": "english"}}},
                    {"name": "fulltext_index_2", "properties": ["property2", "property3"]}
                ],
                "vector_indexes": [
                    {"name": "vector_index_1", "property": "property1", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                ]
            }
        }
        """
                        .stripIndent();

        var target = mapper.readValue(json, RelationshipTarget.class);

        assertThat(target.getName()).isEqualTo("my-relationship-target");
        assertThat(target.isActive()).isFalse();
        assertThat(target.getSource()).isEqualTo("a-source");
        assertThat(target.getDependencies()).isEqualTo(List.of("an-action-or-target"));
        assertThat(target.getWriteMode()).isEqualTo(WriteMode.MERGE);
        assertThat(target.getNodeMatchMode()).isEqualTo(NodeMatchMode.MATCH);
        assertThat(target.getSourceTransformations())
                .isEqualTo(new SourceTransformations(
                        true,
                        List.of(
                                new Aggregation("SUM(unit_price*quantity)", "total_price"),
                                new Aggregation("SUM(quantity)", "total_quantity")),
                        "column IS NOT NULL",
                        List.of(
                                new OrderBy("column_1", null),
                                new OrderBy("column_2", Order.ASC),
                                new OrderBy("column_3", Order.DESC)),
                        42));
        assertThat(target.getType()).isEqualTo("TYPE");
        assertThat(target.getStartNodeReference()).isEqualTo("a-node-target");
        assertThat(target.getEndNodeReference()).isEqualTo("another-node-target");
        assertThat(target.getProperties())
                .isEqualTo(List.of(
                        new PropertyMapping("field_1", "property1", PropertyType.LOCAL_TIME_ARRAY),
                        new PropertyMapping("field_2", "property2", PropertyType.STRING_ARRAY),
                        new PropertyMapping("field_3", "property3", PropertyType.FLOAT)));
        assertThat(target.getSchema())
                .isEqualTo(new RelationshipSchema(
                        true,
                        List.of(
                                new RelationshipKeyConstraint("key_constraint_1", List.of("property1"), null),
                                new RelationshipKeyConstraint(
                                        "key_constraint_2",
                                        List.of("property2"),
                                        Map.of("indexProvider", "range-1.0"))),
                        List.of(new RelationshipUniqueConstraint(
                                "unique_constraint_1", List.of("property3"), Map.of("indexProvider", "range-1.0"))),
                        List.of(new RelationshipExistenceConstraint("existence_constraint_1", "property3")),
                        List.of(
                                new RelationshipRangeIndex("range_index_1", List.of("property1", "property3")),
                                new RelationshipRangeIndex("range_index_2", List.of("property2"))),
                        List.of(
                                new RelationshipTextIndex(
                                        "text_index_1", "property1", Map.of("indexProvider", "text-2.0")),
                                new RelationshipTextIndex("text_index_2", "property2", null)),
                        List.of(
                                new RelationshipPointIndex(
                                        "point_index_1",
                                        "property1",
                                        Map.of(
                                                "indexConfig",
                                                Map.of(
                                                        "spatial.cartesian.min",
                                                        List.of(-100.0, -100.0),
                                                        "spatial.cartesian.max",
                                                        List.of(100.0, 100.0)))),
                                new RelationshipPointIndex("point_index_2", "property2", null)),
                        List.of(
                                new RelationshipFullTextIndex(
                                        "fulltext_index_1",
                                        List.of("property1"),
                                        Map.of("indexConfig", Map.of("fulltext.analyzer", "english"))),
                                new RelationshipFullTextIndex(
                                        "fulltext_index_2", List.of("property2", "property3"), null)),
                        List.of(new RelationshipVectorIndex(
                                "vector_index_1",
                                "property1",
                                Map.of("vector.dimensions", 1536, "vector.similarity_function", "cosine")))));
    }

    @Test
    void compares_unrelated_targets_by_name() {
        List<PropertyMapping> mappings = List.of(new PropertyMapping("src", "prop", null));
        Target nodeTarget = new NodeTarget(
                true, "a-node-target", "a-source", null, WriteMode.CREATE, null, List.of("Label"), mappings, null);
        Target customQueryTarget = new CustomQueryTarget(true, "a-query-target", "a-source", null, "RETURN 42");
        Target relationshipTarget = new RelationshipTarget(
                true,
                "a-relationship-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.CREATE,
                null,
                "start-node-ref",
                "end-node-ref",
                mappings,
                null);

        assertThat(nodeTarget).isLessThan(customQueryTarget).isLessThan(relationshipTarget);
        assertThat(customQueryTarget).isGreaterThan(nodeTarget).isLessThan(relationshipTarget);
        assertThat(relationshipTarget).isGreaterThan(nodeTarget).isGreaterThan(nodeTarget);
    }

    @Test
    void compares_targets_by_dependencies() {
        List<PropertyMapping> mappings = List.of(new PropertyMapping("src", "prop", null));
        Target nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "a-source",
                List.of("a-relationship-target"),
                WriteMode.CREATE,
                null,
                List.of("Label"),
                mappings,
                null);
        Target relationshipTarget = new RelationshipTarget(
                true,
                "a-relationship-target",
                "a-source",
                List.of("a-query-target"),
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.CREATE,
                null,
                "start-node-ref",
                "end-node-ref",
                mappings,
                null);
        Target customQueryTarget = new CustomQueryTarget(
                true, "a-custom-query-target", "a-source", List.of("another-node-target"), "RETURN 42");
        Target anotherNodeTarget = new NodeTarget(
                true,
                "another-node-target",
                "a-source",
                null,
                WriteMode.CREATE,
                null,
                List.of("Label"),
                mappings,
                null);

        assertThat(nodeTarget).isGreaterThan(relationshipTarget);
        assertThat(relationshipTarget).isLessThan(nodeTarget).isGreaterThan(customQueryTarget);
        assertThat(customQueryTarget).isLessThan(relationshipTarget).isGreaterThan(anotherNodeTarget);
        assertThat(anotherNodeTarget).isLessThan(customQueryTarget);
    }

    @Test
    void compares_targets_by_start_node_reference() {
        List<PropertyMapping> mappings = List.of(new PropertyMapping("src", "prop", null));
        Target nodeTarget = new NodeTarget(
                true, "a-node-target", "a-source", null, WriteMode.CREATE, null, List.of("Label"), mappings, null);
        Target relationshipTarget = new RelationshipTarget(
                true,
                "a-relationship-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.CREATE,
                null,
                "a-node-target",
                "end-node-ref",
                mappings,
                null);

        assertThat(nodeTarget).isLessThan(relationshipTarget);
        assertThat(relationshipTarget).isGreaterThan(nodeTarget);
    }

    @Test
    void compares_targets_by_end_node_reference() {
        List<PropertyMapping> mappings = List.of(new PropertyMapping("src", "prop", null));
        Target nodeTarget = new NodeTarget(
                true, "a-node-target", "a-source", null, WriteMode.CREATE, null, List.of("Label"), mappings, null);
        Target relationshipTarget = new RelationshipTarget(
                true,
                "a-relationship-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.CREATE,
                null,
                "start-node-ref",
                "a-node-target",
                mappings,
                null);

        assertThat(nodeTarget).isLessThan(relationshipTarget);
        assertThat(relationshipTarget).isGreaterThan(nodeTarget);
    }
}
