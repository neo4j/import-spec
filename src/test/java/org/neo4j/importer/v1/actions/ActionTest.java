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
package org.neo4j.importer.v1.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @Test
    void deserializes_minimal_HTTP_GET_action() throws Exception {
        var json =
                """
        {
            "name": "my-minimal-http-get-action",
            "type": "http",
            "method": "get",
            "url": "https://example.com"
        }
        """
                        .stripIndent();

        var action = mapper.readValue(json, HttpAction.class);

        assertThat(action.getName()).isEqualTo("my-minimal-http-get-action");
        assertThat(action.isActive()).isTrue();
        assertThat(action.getType()).isEqualTo(ActionType.HTTP);
        assertThat(action.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(action.getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void deserializes_HTTP_POST_action() throws Exception {
        var json =
                """
        {
            "name": "my-http-post-action",
            "active": "false",
            "type": "http",
            "method": "post",
            "url": "https://example.com",
            "headers": {"header_1": "value_1", "header_2": "value_2"},
            "stage": "start"
        }
        """
                        .stripIndent();

        var action = mapper.readValue(json, HttpAction.class);

        assertThat(action.getName()).isEqualTo("my-http-post-action");
        assertThat(action.isActive()).isFalse();
        assertThat(action.getType()).isEqualTo(ActionType.HTTP);
        assertThat(action.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(action.getUrl()).isEqualTo("https://example.com");
        assertThat(action.getHeaders()).isEqualTo(Map.of("header_1", "value_1", "header_2", "value_2"));
        assertThat(action.getStage()).isEqualTo(ActionStage.START);
    }

    @Test
    void deserializes_minimal_Cypher_action() throws Exception {
        var json =
                """
        {
            "name": "my-minimal-cypher-action",
            "type": "cypher",
            "query": "CREATE ()"
        }
        """
                        .stripIndent();

        var action = mapper.readValue(json, CypherAction.class);

        assertThat(action.getName()).isEqualTo("my-minimal-cypher-action");
        assertThat(action.isActive()).isTrue();
        assertThat(action.getType()).isEqualTo(ActionType.CYPHER);
        assertThat(action.getQuery()).isEqualTo("CREATE ()");
        assertThat(action.getExecutionMode()).isEqualTo(CypherExecutionMode.TRANSACTION);
    }

    @Test
    void deserializes_Cypher_action() throws Exception {
        var json =
                """
        {
            "name": "my-cypher-action",
            "active": "false",
            "type": "cypher",
            "query": "CREATE ()",
            "stage": "end",
            "execution_mode": "autocommit"
        }
        """
                        .stripIndent();

        var action = mapper.readValue(json, CypherAction.class);

        assertThat(action.getName()).isEqualTo("my-cypher-action");
        assertThat(action.isActive()).isFalse();
        assertThat(action.getType()).isEqualTo(ActionType.CYPHER);
        assertThat(action.getQuery()).isEqualTo("CREATE ()");
        assertThat(action.getStage()).isEqualTo(ActionStage.END);
        assertThat(action.getExecutionMode()).isEqualTo(CypherExecutionMode.AUTOCOMMIT);
    }
}
