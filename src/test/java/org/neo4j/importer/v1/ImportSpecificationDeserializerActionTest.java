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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.UndeserializableActionException;

// This exercises the compliance of various import spec payloads with the JSON schema
// The class focuses on (lack of) compliance of the action side of the spec.
public class ImportSpecificationDeserializerActionTest {

    @Test
    void fails_if_actions_are_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": 42
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions: integer found, array expected");
    }

    @Test
    void fails_if_action_in_array_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [42]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.actions[0]: integer found, object expected");
    }

    @Test
    void fails_if_action_active_attribute_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "cypher",
                                "active": 42,
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.actions[0].active: integer found, boolean expected");
    }

    @Test
    void fails_if_action_is_missing_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0]: required property 'type' not found");
    }

    @Test
    void fails_if_action_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": 42,
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0].type: integer found, string expected");
    }

    @Test
    void fails_if_action_type_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "foobar",
                                "stage": "start",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining("Expected exactly one action provider for action of type foobar, but found: 0");
    }

    @Test
    void fails_if_action_stage_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "cypher",
                                "stage": 42,
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.actions[0].stage: integer found, string expected");
    }

    @Test
    void fails_if_action_stage_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "type": "cypher",
                                "stage": "foobar",
                                "query": "CREATE INDEX FOR (n:ANode) ON n.name"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.actions[0].stage: does not have a value in the enumeration [start, post_sources, pre_nodes, post_nodes, pre_relationships, post_relationships, pre_queries, post_queries, end]");
    }

    @Test
    void fails_if_cypher_action_execution_mode_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "stage": "start",
                                "type": "cypher",
                                "query": "RETURN 42",
                                "execution_mode": 42
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining(
                        "Action provider org.neo4j.importer.v1.actions.plugin.CypherActionProvider failed to deserialize");
    }

    @Test
    void fails_if_cypher_action_execution_mode_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
                        {
                            "version": "1",
                            "sources": [{
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }],
                            "targets": {
                                "queries": [{
                                    "name": "a-target",
                                    "source": "a-source",
                                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                                }]
                            },
                            "actions": [{
                                "name": "an-action",
                                "stage": "start",
                                "type": "cypher",
                                "query": "RETURN 42",
                                "execution_mode": "foobar"
                            }]
                        }
                        """
                                .stripIndent())))
                .isInstanceOf(UndeserializableActionException.class)
                .hasMessageContaining(
                        "Action provider org.neo4j.importer.v1.actions.plugin.CypherActionProvider failed to deserialize");
    }
}
