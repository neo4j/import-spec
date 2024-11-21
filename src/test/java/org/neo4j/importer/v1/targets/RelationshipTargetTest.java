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

class RelationshipTargetTest {

    private final Random random = new Random();

    @Test
    void returns_no_keys_when_schema_is_not_defined() {
        RelationshipSchema schema = null;
        var target = new RelationshipTarget(
                true,
                "a-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.MERGE,
                null,
                "a-node-target",
                "a-node-target",
                List.of(mappingTo("prop")),
                schema);

        assertThat(target.getKeyProperties()).isEmpty();
    }

    @Test
    void returns_key_properties() {
        var properties = List.of(mappingTo("prop1"), mappingTo("prop2"), mappingTo("prop3"), mappingTo("prop4"));
        var schema = schemaFor(List.of(key(List.of("prop1", "prop2")), key(List.of("prop2", "prop4"))));
        var target = new RelationshipTarget(
                true,
                "a-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.MATCH,
                null,
                "start-node-target",
                "end-node-target",
                properties,
                schema);

        assertThat(target.getKeyProperties()).isEqualTo(List.of("prop1", "prop2", "prop4"));
    }

    @Test
    void returns_non_null_unique_properties() {
        var properties = List.of(mappingTo("prop1"), mappingTo("prop2"), mappingTo("prop3"), mappingTo("prop4"));
        var schema = schemaFor(
                List.of(unique(List.of("prop1", "prop2")), unique(List.of("prop2", "prop4"))),
                List.of(notNull("prop2"), notNull("prop3"), notNull("prop4")));
        var target = new RelationshipTarget(
                true,
                "a-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.MATCH,
                null,
                "start-node-target",
                "end-node-target",
                properties,
                schema);

        assertThat(target.getKeyProperties()).isEqualTo(List.of("prop2", "prop4"));
    }

    @Test
    void returns_both_key_and_non_null_unique_properties() {
        var properties = List.of(mappingTo("prop1"), mappingTo("prop2"), mappingTo("prop3"), mappingTo("prop4"));
        var schema = schemaFor(
                List.of(key(List.of("prop1", "prop2"))),
                List.of(unique(List.of("prop3"))),
                List.of(notNull("prop3"), notNull("prop4")));
        var target = new RelationshipTarget(
                true,
                "a-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.MATCH,
                null,
                "start-node-target",
                "end-node-target",
                properties,
                schema);

        assertThat(target.getKeyProperties()).isEqualTo(List.of("prop1", "prop2", "prop3"));
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
}
