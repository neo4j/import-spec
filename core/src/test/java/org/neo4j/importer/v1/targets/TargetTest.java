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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.importer.v1.SpecFormat;

class TargetTest {

    private final YAMLMapper mapper = YAMLMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_minimal_custom_query_target(SpecFormat format, TestInfo testInfo) throws Exception {
        try (var reader = targetReader(format, testInfo)) {
            var target = mapper.readValue(reader, CustomQueryTarget.class);

            assertThat(target.getTargetType()).isEqualTo(TargetType.QUERY);
            assertThat(target.getName()).isEqualTo("my-minimal-custom-query-target");
            assertThat(target.isActive()).isTrue();
            assertThat(target.getSource()).isEqualTo("a-source");
            assertThat(target.getQuery()).isEqualTo("UNWIND $rows AS row CREATE (n:ANode) SET n = row");
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_custom_query_target(SpecFormat format, TestInfo testInfo) throws Exception {
        try (var reader = targetReader(format, testInfo)) {
            var target = mapper.readValue(reader, CustomQueryTarget.class);

            assertThat(target.getTargetType()).isEqualTo(TargetType.QUERY);
            assertThat(target.getName()).isEqualTo("my-custom-query-target");
            assertThat(target.isActive()).isFalse();
            assertThat(target.getSource()).isEqualTo("a-source");
            assertThat(target.getDependencies()).isEqualTo(List.of("another-action-or-target"));
            assertThat(target.getQuery()).isEqualTo("UNWIND $rows AS row CREATE (n:ANode) SET n = row");
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_minimal_node_target(SpecFormat format, TestInfo testInfo) throws Exception {
        try (var reader = targetReader(format, testInfo)) {
            var target = mapper.readValue(reader, NodeTarget.class);

            assertThat(target.getTargetType()).isEqualTo(TargetType.NODE);
            assertThat(target.getName()).isEqualTo("my-minimal-node-target");
            assertThat(target.isActive()).isTrue();
            assertThat(target.getSource()).isEqualTo("a-source");
            assertThat(target.getLabels()).isEqualTo(List.of("Label1", "Label2"));
            assertThat(target.getProperties())
                    .isEqualTo(List.of(
                            new PropertyMapping("field_1", "property1", null),
                            new PropertyMapping("field_2", "property2", null)));
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_node_target(SpecFormat format, TestInfo testInfo) throws Exception {
        try (var reader = targetReader(format, testInfo)) {
            var target = mapper.readValue(reader, NodeTarget.class);

            assertThat(target.getTargetType()).isEqualTo(TargetType.NODE);
            assertThat(target.getName()).isEqualTo("my-node-target");
            assertThat(target.isActive()).isFalse();
            assertThat(target.getSource()).isEqualTo("a-source");
            assertThat(target.getDependencies()).isEqualTo(List.of("an-action-or-target"));
            assertThat(target.getWriteMode()).isEqualTo(WriteMode.MERGE);
            var sourceTransformations = target.getExtension(SourceTransformations.class);
            assertThat(sourceTransformations).isPresent();
            assertThat(sourceTransformations.get())
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
                            List.of(new NodeTypeConstraint("type_constraint_1", "Label1", "property1")),
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
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_minimal_relationship_target(SpecFormat format, TestInfo testInfo) throws Exception {
        try (var reader = targetReader(format, testInfo)) {
            var target = mapper.readValue(reader, RelationshipTarget.class);

            assertThat(target.getTargetType()).isEqualTo(TargetType.RELATIONSHIP);
            assertThat(target.getStartNodeReference().getName()).isEqualTo("a-node-target");
            assertThat(target.getStartNodeReference().getKeyMappings()).isEmpty();
            assertThat(target.getEndNodeReference().getName()).isEqualTo("another-node-target");
            var keyMappings = target.getEndNodeReference().getKeyMappings();
            assertThat(keyMappings).hasSize(1);
            var keyMapping = keyMappings.get(0);
            assertThat(keyMapping.getSourceField()).isEqualTo("source_id");
            assertThat(keyMapping.getNodeProperty()).isEqualTo("target_id");
        }
    }

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_relationship_target(SpecFormat format, TestInfo testInfo) throws Exception {
        try (var reader = targetReader(format, testInfo)) {
            var target = mapper.readValue(reader, RelationshipTarget.class);

            assertThat(target.getTargetType()).isEqualTo(TargetType.RELATIONSHIP);
            assertThat(target.getName()).isEqualTo("my-relationship-target");
            assertThat(target.isActive()).isFalse();
            assertThat(target.getSource()).isEqualTo("a-source");
            assertThat(target.getDependencies()).isEqualTo(List.of("an-action-or-target"));
            assertThat(target.getWriteMode()).isEqualTo(WriteMode.MERGE);
            assertThat(target.getNodeMatchMode()).isEqualTo(NodeMatchMode.MATCH);
            var sourceTransformations = target.getExtension(SourceTransformations.class);
            assertThat(sourceTransformations).isPresent();
            assertThat(sourceTransformations.get())
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
            assertThat(target.getStartNodeReference().getName()).isEqualTo("a-node-target");
            assertThat(target.getEndNodeReference().getName()).isEqualTo("another-node-target");
            assertThat(target.getEndNodeReference().getKeyMappings()).hasSize(1);
            assertThat(target.getEndNodeReference().getKeyMappings().get(0).getSourceField())
                    .isEqualTo("source_id");
            assertThat(target.getEndNodeReference().getKeyMappings().get(0).getNodeProperty())
                    .isEqualTo("target_id");
            assertThat(target.getProperties())
                    .isEqualTo(List.of(
                            new PropertyMapping("field_1", "property1", PropertyType.LOCAL_TIME_ARRAY),
                            new PropertyMapping("field_2", "property2", PropertyType.STRING_ARRAY),
                            new PropertyMapping("field_3", "property3", PropertyType.FLOAT)));
            assertThat(target.getSchema())
                    .isEqualTo(new RelationshipSchema(
                            List.of(new RelationshipTypeConstraint("type_constraint_1", "property1")),
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
    }

    private static FileReader targetReader(SpecFormat format, TestInfo testInfo) {
        var spec = String.format(
                "/specs/target_test/%s.%s",
                testInfo.getTestMethod().orElseThrow().getName(), format.extension());
        var resourceUrl = TargetTest.class.getResource(spec);
        assertThat(resourceUrl).isNotNull();
        try {
            return new FileReader(Path.of(resourceUrl.toURI()).toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
