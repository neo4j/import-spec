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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class TargetsTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @Test
    void deserializes_targets() throws Exception {
        var json = """
                {
                  "nodes": [
                    {
                      "name": "my-minimal-node-target",
                      "source": "a-source",
                      "write_mode": "create",
                      "labels": [
                        "Label1",
                        "Label2"
                      ],
                      "properties": [
                        {
                          "source_field": "field_1",
                          "target_property": "property1"
                        },
                        {
                          "source_field": "field_2",
                          "target_property": "property2"
                        }
                      ]
                    }
                  ],
                  "relationships": [
                    {
                      "name": "my-minimal-relationship-target",
                      "source": "a-source",
                      "write_mode": "create",
                      "node_match_mode": "match",
                      "type": "TYPE",
                      "start_node_reference": "my-minimal-node-target",
                      "end_node_reference": "my-minimal-node-target",
                      "properties": [
                        {
                          "source_field": "field_1",
                          "target_property": "property1"
                        },
                        {
                          "source_field": "field_2",
                          "target_property": "property2"
                        }
                      ]
                    }
                  ],
                  "queries": [
                    {
                      "name": "my-minimal-custom-query-target",
                      "source": "a-source",
                      "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                    }
                  ]
                }
                """.stripIndent();

        Targets targets = mapper.readValue(json, Targets.class);

        assertThat(targets.getNodes())
                .isEqualTo(List.of(new NodeTarget(
                        true,
                        "my-minimal-node-target",
                        "a-source",
                        null,
                        WriteMode.CREATE,
                        (ObjectNode) null,
                        List.of("Label1", "Label2"),
                        List.of(
                                new PropertyMapping("field_1", "property1", null),
                                new PropertyMapping("field_2", "property2", null)),
                        null)));
        assertThat(targets.getRelationships())
                .isEqualTo(List.of(new RelationshipTarget(
                        true,
                        "my-minimal-relationship-target",
                        "a-source",
                        null,
                        "TYPE",
                        WriteMode.CREATE,
                        NodeMatchMode.MATCH,
                        (ObjectNode) null,
                        new NodeReference("my-minimal-node-target"),
                        new NodeReference("my-minimal-node-target"),
                        List.of(
                                new PropertyMapping("field_1", "property1", null),
                                new PropertyMapping("field_2", "property2", null)),
                        null)));
        assertThat(targets.getCustomQueries())
                .isEqualTo(List.of(new CustomQueryTarget(
                        true,
                        "my-minimal-custom-query-target",
                        "a-source",
                        null,
                        "UNWIND $rows AS row CREATE (n:ANode) SET n = row")));
    }
}
