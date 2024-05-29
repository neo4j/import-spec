package org.neo4j.importer.v1;

import org.junit.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;

import java.io.StringReader;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                    "labels": ["Label1", "Label2"],
                    "properties": [
                      {"source_field": "field_1", "target_property": "property1"},
                      {"source_field": "field_2", "target_property": "property2"},
                      {"source_field": "field_3", "target_property": "property3"},
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
                        Optional.of(new Neo4jDistribution("5.0", "ENTERPRISE"))))
                .isInstanceOf(InvalidSpecificationException.class);
    }
}
