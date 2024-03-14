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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.actions.HttpAction;
import org.neo4j.importer.v1.actions.HttpMethod;
import org.neo4j.importer.v1.sources.BigQuerySource;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.Targets;

class ImportSpecificationTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @Test
    void deserializes_minimal_job_spec() throws Exception {
        var json =
                """
            {
                "sources": [{
                    "name": "my-bigquery-source",
                    "type": "bigquery",
                    "query": "SELECT id, name FROM my.table"
                }],
                "targets": {
                    "queries": [{
                        "name": "my-query",
                        "source": "my-bigquery-source",
                        "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                    }]
                }
            }
        """
                        .stripIndent();

        var spec = mapper.readValue(json, ImportSpecification.class);

        assertThat(spec.getConfiguration()).isNull();
        assertThat(spec.getSources())
                .isEqualTo(List.of(new BigQuerySource("my-bigquery-source", "SELECT id, name FROM my.table")));
        assertThat(spec.getTargets())
                .isEqualTo(new Targets(
                        null,
                        null,
                        List.of(new CustomQueryTarget(
                                true,
                                "my-query",
                                "my-bigquery-source",
                                null,
                                "UNWIND $rows AS row CREATE (n:ANode) SET n = row"))));
        assertThat(spec.getActions()).isNull();
    }

    @Test
    void deserializes_job_spec() throws Exception {
        var json =
                """
            {
                "config": {
                    "foo": "bar",
                    "baz": 42,
                    "qix": [true, 1.0, {}]
                },
                "sources": [{
                    "name": "my-bigquery-source",
                    "type": "bigquery",
                    "query": "SELECT id, name FROM my.table"
                }],
                "targets": {
                    "queries": [{
                        "name": "my-query",
                        "source": "my-bigquery-source",
                        "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                    }]
                },
                "actions": [{
                    "name": "my-http-get-action",
                    "type": "http",
                    "method": "get",
                    "url": "https://example.com"
                }]
            }
        """
                        .stripIndent();

        var spec = mapper.readValue(json, ImportSpecification.class);

        assertThat(spec.getConfiguration())
                .isEqualTo(Map.of("foo", "bar", "baz", 42, "qix", List.of(true, 1.0, Map.of())));
        assertThat(spec.getSources())
                .isEqualTo(List.of(new BigQuerySource("my-bigquery-source", "SELECT id, name FROM my.table")));
        assertThat(spec.getTargets())
                .isEqualTo(new Targets(
                        null,
                        null,
                        List.of(new CustomQueryTarget(
                                true,
                                "my-query",
                                "my-bigquery-source",
                                null,
                                "UNWIND $rows AS row CREATE (n:ANode) SET n = row"))));
        assertThat(spec.getActions())
                .isEqualTo(List.of(
                        new HttpAction(true, "my-http-get-action", null, "https://example.com", HttpMethod.GET, null)));
    }
}
