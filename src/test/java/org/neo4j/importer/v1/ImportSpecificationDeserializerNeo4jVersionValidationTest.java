package org.neo4j.importer.v1;

import org.junit.Test;
import org.neo4j.importer.v1.distribution.Neo4jDistributions;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.SpecificationException;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

public class ImportSpecificationDeserializerNeo4jVersionValidationTest {
    @Test
    public void test() throws SpecificationException {
        var json =
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
                                "labels": ["Label1", "Label2"],
                                "properties": [
                                    {"source_field": "field_1", "target_property": "property1"},
                                    {"source_field": "field_2", "target_property": "property2"},
                                    {"source_field": "field_3", "target_property": "property3"}
                                ],
                                "schema": {
                                    "type_constraints": [
                                        {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                    ],
                                    "unique_constraints": [
                                        {"name": "unique_constraint_1", "label": "Label1", "properties": ["property3"], "options": {"indexProvider": "range-1.0"}}
                                    ],
                                    "existence_constraints": [
                                        {"name": "existence_constraint_1", "label": "Label2", "property": "property3"}
                                    ],
                                    "vector_indexes": [
                                        {"name": "vector_index_1", "label": "Label1", "property": "property1", "options": {"vector.dimensions": 1536, "vector.similarity_function": "cosine"}}
                                    ]
                                }
                            },
                            {
                                "name": "a-node-target-1",
                                "source": "a-source",
                                "labels": ["Label1", "Label2"],
                                "properties": [
                                    {"source_field": "field_1", "target_property": "property1"},
                                    {"source_field": "field_2", "target_property": "property2"},
                                    {"source_field": "field_3", "target_property": "property3"}
                                ],
                                "schema": {
                                    "type_constraints": [
                                        {"name": "type_constraint_1", "label": "Label1", "property": "property1"}
                                    ],
                                    "unique_constraints": [
                                        {"name": "unique_constraint_1", "label": "Label1", "properties": ["property3"], "options": {"indexProvider": "range-1.0"}}
                                    ],
                                    "existence_constraints": [
                                        {"name": "existence_constraint_1", "label": "Label2", "property": "property3"}
                                    ]
                                }
                            }
                        ]
                    }
                }
                """;

        deserialize(new StringReader(json), Neo4jDistributions.COMMUNITY.of("4.4"));
    }

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
                            "labels": ["Label1", "Label2"],
                            "properties": [
                                {"source_field": "field_1", "target_property": "property1"},
                                {"source_field": "field_2", "target_property": "property2"},
                                {"source_field": "field_3", "target_property": "property3"}
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
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "[$.targets.nodes[0].schema] [type_constraints], features are not supported by Neo4j 5.0 ENTERPRISE.");
    }
}
