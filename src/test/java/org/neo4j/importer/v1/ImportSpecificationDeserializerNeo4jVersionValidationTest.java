package org.neo4j.importer.v1;

import org.junit.Test;
import org.neo4j.importer.v1.distribution.Neo4jDistributions;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

public class ImportSpecificationDeserializerNeo4jVersionValidationTest {

    @Test
    public void fails_if_version_below_5_9_with_node_type_constraint() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader(
                                """
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                ]
                            }
                        }
                    ]
                }
            }
            """
                                        .stripIndent()),
                        Neo4jDistributions.ENTERPRISE.of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] [type_constraints], features are not supported by Neo4j 5.0 ENTERPRISE.");
    }

    @Test
    public void fails_if_edition_community_with_node_key_constraint() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader(
                                """
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"},
                            ],
                            "schema": {
                                "key_constraints": [
                                    {"name": "key_constraint_1", "label": "Label1", "properties": ["property1"]}
                                ]
                            }
                        }
                    ]
                }
            }
            """
                                        .stripIndent()),
                        Neo4jDistributions.COMMUNITY.of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] [key_constraints], features are not supported by Neo4j 5.0 COMMUNITY.");
    }

    @Test
    public void fails_if_version_below_5_13_with_node_vector_index() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader(
                                """
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"}
                            ],
                            "schema": {
                                "vector_indexes": [
                                    {"name": "vector_index_1", "label": "Label1", "property": "property1", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                ]
                            }
                        }
                    ]
                }
            }
            """
                                        .stripIndent()),
                        Neo4jDistributions.ENTERPRISE.of("5.12")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.nodes[0].schema] [vector_indexes], features are not supported by Neo4j 5.12 ENTERPRISE.");
    }

    @Test
    public void fails_if_version_below_5_9_with_relationship_type_constraint() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader(
                                """
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"}
                            ]
                        },
                        {
                            "name": "another-node-target",
                            "source": "a-source",
                            "labels": ["Label2"],
                            "properties": [
                                {"source_field": "field_2", "target_property": "property2"}
                            ]
                        }
                    ],
                    "relationships": [
                        {
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "another-node-target",
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"},
                                {"source_field": "field_2", "target_property": "property2"},
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_1", "property": "property1"}
                                ]
                            }
                        }
                    ]
                }
            }

            """
                                        .stripIndent()),
                        Neo4jDistributions.ENTERPRISE.of("5.8")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "[$.targets.relationships[0].schema] [type_constraints], features are not supported by Neo4j 5.8 ENTERPRISE.");
    }

    @Test
    public void fails_if_below_the_version_with_multiple_nodes_and_relationships() {
        assertThatThrownBy(() -> deserialize(
                        new StringReader(
                                """
            {
                "version": "1",
                "sources": [
                    {
                        "name": "a-source",
                        "type": "bigquery",
                        "query": "SELECT id, name FROM my.table"
                    }
                ],
                "targets": {
                    "nodes": [
                        {
                            "name": "a-node-target",
                            "source": "a-source",
                            "labels": ["Label1"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                ],
                                "existence_constraints": [
                                    {"name": "existence_constraint_1", "label": "Label1", "property": "property1"}
                                ]
                            }
                        },
                        {
                            "name": "another-node-target",
                            "source": "a-source",
                            "labels": ["Label2"],
                            "properties": [
                                {"source_field": "field_2", "target_property": "property2"}
                            ],
                            "schema": {
                                "vector_indexes": [
                                    {"name": "vector_index_1", "label": "Label2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                ]
                            }
                        }
                    ],
                    "relationships": [
                        {
                            "name": "a-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "another-node-target",
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"}
                            ],
                            "schema": {
                                "type_constraints": [
                                    {"name": "type_constraint_1", "property": "property1"}
                                ]
                            }
                        },
                        {
                            "name": "another-relationship-target",
                            "source": "a-source",
                            "type": "TYPE",
                            "start_node_reference": "a-node-target",
                            "end_node_reference": "another-node-target",
                            "properties": [
                                {"source_field": "field_2", "target_property": "property2"}
                            ],
                            "schema": {
                                "vector_indexes": [
                                    {"name": "vector_index_2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                ]
                            }
                        }
                    ]
                }
            }
            """
                                        .stripIndent()),
                        Neo4jDistributions.COMMUNITY.of("5.0")))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll(
                        "4 error(s)",
                        "0 warning(s)",
                        "[VERS-001][$.targets.nodes[0].schema] [type_constraints, existence_constraints], features are not supported by Neo4j 5.0 COMMUNITY.",
                        "[VERS-001][$.targets.nodes[1].schema] [vector_indexes], features are not supported by Neo4j 5.0 COMMUNITY.",
                        "[VERS-001][$.targets.relationships[0].schema] [type_constraints], features are not supported by Neo4j 5.0 COMMUNITY.",
                        "[VERS-001][$.targets.relationships[1].schema] [vector_indexes], features are not supported by Neo4j 5.0 COMMUNITY.");
    }

    @Test
    public void doesnt_fails_if_above_the_version() {
        assertDoesNotThrow(() -> deserialize(
                    new StringReader(
                            """
                    {
                        "version": "1",
                        "sources": [
                            {
                                "name": "a-source",
                                "type": "bigquery",
                                "query": "SELECT id, name FROM my.table"
                            }
                        ],
                        "targets": {
                            "nodes": [
                                {
                                    "name": "a-node-target",
                                    "source": "a-source",
                                    "labels": ["Label1"],
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"}
                                    ],
                                    "schema": {
                                        "type_constraints": [
                                            {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                        ],
                                        "existence_constraints": [
                                            {"name": "existence_constraint_1", "label": "Label1", "property": "property1"}
                                        ]
                                    }
                                },
                                {
                                    "name": "another-node-target",
                                    "source": "a-source",
                                    "labels": ["Label2"],
                                    "properties": [
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "vector_index_1", "label": "Label2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                        ]
                                    }
                                }
                            ],
                            "relationships": [
                                {
                                    "name": "a-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_1", "target_property": "property1"}
                                    ],
                                    "schema": {
                                        "type_constraints": [
                                            {"name": "type_constraint_1", "property": "property1"}
                                        ]
                                    }
                                },
                                {
                                    "name": "another-relationship-target",
                                    "source": "a-source",
                                    "type": "TYPE",
                                    "start_node_reference": "a-node-target",
                                    "end_node_reference": "another-node-target",
                                    "properties": [
                                        {"source_field": "field_2", "target_property": "property2"}
                                    ],
                                    "schema": {
                                        "vector_indexes": [
                                            {"name": "vector_index_2", "property": "property2", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                    """
                                .stripIndent()),
                Neo4jDistributions.ENTERPRISE.of("5.20")));
    }
}
