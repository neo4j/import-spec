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
package org.neo4j.importer.v1.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.targets.NodeMatchMode;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipExistenceConstraint;
import org.neo4j.importer.v1.targets.RelationshipKeyConstraint;
import org.neo4j.importer.v1.targets.RelationshipSchema;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.RelationshipUniqueConstraint;
import org.neo4j.importer.v1.targets.WriteMode;

class RelationshipTargetStepTest {

    private final Random random = new Random();

    private final PropertyMapping mapping1 = mappingTo("prop1");
    private final PropertyMapping mapping2 = mappingTo("prop2");
    private final PropertyMapping mapping3 = mappingTo("prop3");
    private final PropertyMapping mapping4 = mappingTo("prop4");
    private final List<PropertyMapping> properties = List.of(mapping1, mapping2, mapping3, mapping4);

    @Test
    void returns_no_properties_when_schema_is_not_defined() {
        RelationshipSchema schema = null;

        var task = new RelationshipTargetStep(
                new RelationshipTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        "TYPE",
                        WriteMode.CREATE,
                        NodeMatchMode.MERGE,
                        null,
                        new NodeReference("a-node-target"),
                        new NodeReference("a-node-target"),
                        properties,
                        schema),
                nodeTarget("a-node-target"),
                nodeTarget("a-node-target"),
                List.of());

        assertThat(task.keyProperties()).isEmpty();
        assertThat(task.nonKeyProperties()).isEqualTo(properties);
    }

    @Test
    void returns_key_and_non_key_properties() {
        var schema = schemaFor(List.of(key(List.of("prop1", "prop2")), key(List.of("prop2", "prop4"))));

        var task = new RelationshipTargetStep(
                new RelationshipTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        "TYPE",
                        WriteMode.CREATE,
                        NodeMatchMode.MATCH,
                        null,
                        new NodeReference("start-node-target"),
                        new NodeReference("end-node-target"),
                        properties,
                        schema),
                nodeTarget("start-node-target"),
                nodeTarget("end-node-target"),
                List.of());

        assertThat(task.keyProperties()).containsExactly(mapping1, mapping2, mapping4);
        assertThat(task.nonKeyProperties()).containsExactly(mapping3);
    }

    @Test
    void returns_non_null_unique_properties_as_keys() {
        var schema = schemaFor(
                List.of(unique(List.of("prop1", "prop2")), unique(List.of("prop2", "prop4"))),
                List.of(notNull("prop2"), notNull("prop3"), notNull("prop4")));

        var task = new RelationshipTargetStep(
                new RelationshipTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        "TYPE",
                        WriteMode.CREATE,
                        NodeMatchMode.MATCH,
                        null,
                        new NodeReference("start-node-target"),
                        new NodeReference("end-node-target"),
                        properties,
                        schema),
                nodeTarget("start-node-target"),
                nodeTarget("end-node-target"),
                List.of());

        assertThat(task.keyProperties()).containsExactly(mapping2, mapping4);
        assertThat(task.nonKeyProperties()).containsExactly(mapping1, mapping3);
    }

    @Test
    void returns_both_key_and_non_null_unique_properties() {
        var schema = schemaFor(
                List.of(key(List.of("prop1", "prop2"))),
                List.of(unique(List.of("prop3"))),
                List.of(notNull("prop3"), notNull("prop4")));

        var task = new RelationshipTargetStep(
                new RelationshipTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        "TYPE",
                        WriteMode.CREATE,
                        NodeMatchMode.MATCH,
                        null,
                        new NodeReference("start-node-target"),
                        new NodeReference("end-node-target"),
                        properties,
                        schema),
                nodeTarget("start-node-target"),
                nodeTarget("end-node-target"),
                List.of());

        assertThat(task.keyProperties()).containsExactly(mapping1, mapping2, mapping3);
        assertThat(task.nonKeyProperties()).containsExactly(mapping4);
    }

    private RelationshipKeyConstraint key(List<String> properties) {
        return new RelationshipKeyConstraint("key-%d".formatted(random.nextInt()), properties, null);
    }

    private RelationshipUniqueConstraint unique(List<String> properties) {
        return new RelationshipUniqueConstraint("unique-%d".formatted(random.nextInt()), properties, null);
    }

    private RelationshipExistenceConstraint notNull(String property) {
        return new RelationshipExistenceConstraint("not-null-%d".formatted(random.nextInt()), property);
    }

    private PropertyMapping mappingTo(String name) {
        return new PropertyMapping("a-column-%d".formatted(random.nextInt()), name, null);
    }

    private static RelationshipSchema schemaFor(List<RelationshipKeyConstraint> keys) {
        return schemaFor(keys, null, null);
    }

    private static RelationshipSchema schemaFor(
            List<RelationshipUniqueConstraint> uniques, List<RelationshipExistenceConstraint> notNulls) {
        return schemaFor(null, uniques, notNulls);
    }

    private static RelationshipSchema schemaFor(
            List<RelationshipKeyConstraint> keys,
            List<RelationshipUniqueConstraint> uniques,
            List<RelationshipExistenceConstraint> notNulls) {
        return new RelationshipSchema(null, keys, uniques, notNulls, null, null, null, null, null);
    }

    private static NodeTargetStep nodeTarget(String name) {
        return new NodeTargetStep(
                new NodeTarget(
                        true,
                        name,
                        "a-source",
                        null,
                        WriteMode.CREATE,
                        (ObjectNode) null,
                        List.of("Label"),
                        List.of(new PropertyMapping("a-field", "a-property", null)),
                        null),
                List.of());
    }
}
