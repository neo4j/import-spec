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
import org.junit.jupiter.api.Test;

class EntityTargetTest {

    @Test
    void returns_no_relationship_properties_when_they_are_not_defined() {
        List<PropertyMapping> properties = null;
        var target = new RelationshipTarget(
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
                null);

        assertThat(target.getProperties()).isEmpty();
    }
}
