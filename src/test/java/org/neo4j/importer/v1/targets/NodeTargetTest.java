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

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NodeTargetTest {
    private final Random random = new Random();

    @Test
    void returns_no_keys_when_schema_is_not_defined() {
        NodeSchema schema = null;
        var target = new NodeTarget(
                true,
                "a-target",
                "a-source",
                null,
                WriteMode.CREATE,
                null,
                List.of("Label"),
                List.of(mappingTo("prop")),
                schema);

        assertThat(target.getKeyProperties()).isEmpty();
    }

    @Test
    void returns_key_properties() {
        var properties = List.of(mappingTo("prop1"), mappingTo("prop2"), mappingTo("prop3"), mappingTo("prop4"));
        var schema =
                schemaFor(List.of(key("Label", List.of("prop1", "prop2")), key("Label", List.of("prop2", "prop4"))));
        var target = new NodeTarget(
                true, "a-target", "a-source", null, WriteMode.CREATE, null, List.of("Label"), properties, schema);

        assertThat(target.getKeyProperties()).isEqualTo(List.of("prop1", "prop2", "prop4"));
    }

    @Test
    void returns_non_null_unique_properties() {
        var properties = List.of(mappingTo("prop1"), mappingTo("prop2"), mappingTo("prop3"), mappingTo("prop4"));
        var schema = schemaFor(
                List.of(unique("Label", List.of("prop1", "prop2")), unique("Label", List.of("prop2", "prop4"))),
                List.of(notNull("Label", "prop2"), notNull("Label", "prop3"), notNull("Label", "prop4")));
        var target = new NodeTarget(
                true, "a-target", "a-source", null, WriteMode.CREATE, null, List.of("Label"), properties, schema);

        assertThat(target.getKeyProperties()).isEqualTo(List.of("prop2", "prop4"));
    }

    @Test
    void returns_both_key_and_non_null_unique_properties() {
        var properties = List.of(mappingTo("prop1"), mappingTo("prop2"), mappingTo("prop3"), mappingTo("prop4"));
        var schema = schemaFor(
                List.of(key("Label", List.of("prop1", "prop2"))),
                List.of(unique("Label", List.of("prop3"))),
                List.of(notNull("Label", "prop3"), notNull("Label", "prop4")));
        var target = new NodeTarget(
                true, "a-target", "a-source", null, WriteMode.CREATE, null, List.of("Label"), properties, schema);

        assertThat(target.getKeyProperties()).isEqualTo(List.of("prop1", "prop2", "prop3"));
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
