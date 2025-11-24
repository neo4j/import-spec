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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.targets.NodeExistenceConstraint;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeUniqueConstraint;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.WriteMode;

class NodeTargetStepTest {

    private final Random random = new Random();

    private final PropertyMapping prop1 = mappingTo("prop1");
    private final PropertyMapping prop2 = mappingTo("prop2");
    private final PropertyMapping prop3 = mappingTo("prop3");
    private final PropertyMapping prop4 = mappingTo("prop4");
    private final List<PropertyMapping> properties = List.of(prop1, prop2, prop3, prop4);

    @Test
    void returns_key_and_non_key_properties() {
        var schema =
                schemaFor(List.of(key("Label", List.of("prop1", "prop2")), key("Label", List.of("prop2", "prop4"))));

        var task = new NodeTargetStep(
                new NodeTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        WriteMode.CREATE,
                        (ObjectNode) null,
                        List.of("Label"),
                        properties,
                        schema),
                Set.of());

        assertThat(task.keyProperties()).containsExactly(prop1, prop2, prop4);
        assertThat(task.nonKeyProperties()).containsExactly(prop3);
    }

    @Test
    void returns_unique_properties_as_keys_when_no_key_constraints_are_defined() {
        var schema = schemaFor(
                List.of(unique("Label", List.of("prop1", "prop2")), unique("Label", List.of("prop2", "prop4"))),
                List.of(notNull("Label", "prop2"), notNull("Label", "prop3"), notNull("Label", "prop4")));

        var task = new NodeTargetStep(
                new NodeTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        WriteMode.CREATE,
                        (ObjectNode) null,
                        List.of("Label"),
                        properties,
                        schema),
                Set.of());

        assertThat(task.keyProperties()).containsExactly(prop1, prop2, prop4);
        assertThat(task.nonKeyProperties()).containsExactly(prop3);
    }

    @Test
    void returns_key_over_unique_properties() {
        var schema = schemaFor(
                List.of(key("Label", List.of("prop1", "prop2"))),
                List.of(unique("Label", List.of("prop3"))),
                List.of(notNull("Label", "prop3"), notNull("Label", "prop4")));

        var task = new NodeTargetStep(
                new NodeTarget(
                        true,
                        "a-target",
                        "a-source",
                        null,
                        WriteMode.CREATE,
                        (ObjectNode) null,
                        List.of("Label"),
                        properties,
                        schema),
                Set.of());

        assertThat(task.keyProperties()).containsExactly(prop1, prop2);
        assertThat(task.nonKeyProperties()).containsExactly(prop3, prop4);
    }

    private NodeKeyConstraint key(String label, List<String> properties) {
        return new NodeKeyConstraint("key-%d".formatted(random.nextInt()), label, properties, null);
    }

    private NodeUniqueConstraint unique(String label, List<String> properties) {
        return new NodeUniqueConstraint("unique-%d".formatted(random.nextInt()), label, properties, null);
    }

    private NodeExistenceConstraint notNull(String label, String property) {
        return new NodeExistenceConstraint("not-null-%d".formatted(random.nextInt()), label, property);
    }

    private PropertyMapping mappingTo(String name) {
        return new PropertyMapping("a-column-%d".formatted(random.nextInt()), name, null);
    }

    private static NodeSchema schemaFor(List<NodeKeyConstraint> keys) {
        return schemaFor(keys, null, null);
    }

    private static NodeSchema schemaFor(List<NodeUniqueConstraint> uniques, List<NodeExistenceConstraint> notNulls) {
        return schemaFor(null, uniques, notNulls);
    }

    private static NodeSchema schemaFor(
            List<NodeKeyConstraint> keys, List<NodeUniqueConstraint> uniques, List<NodeExistenceConstraint> notNulls) {
        return new NodeSchema(null, keys, uniques, notNulls, null, null, null, null, null);
    }
}
