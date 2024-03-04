package org.neo4j.importer.v1;


import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

// This exercises the compliance of various import spec payloads with the JSON schema
// The class focuses on (lack of) compliance the source side of the spec.
public class ImportSpecificationDeserializerSourceTest {

    @Test
    void fails_if_source_is_missing_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0]: required property 'type' not found");
    }

    @Test
    void fails_if_source_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": 42,
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0].type: integer found, string expected");
    }

    @Test
    void fails_if_source_type_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "unsupported",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0].type: integer found, string expected");
    }

    @Test
    void fails_if_bigquery_source_lacks_query() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "bigquery"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0]: required property 'query' not found");
    }

    @Test
    void fails_if_bigquery_source_query_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "bigquery",
                "query": 42
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0].query: integer found, string expected");
    }

    @Test
    void fails_if_jdbc_data_source_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "query": "SELECT p.productname FROM products p ORDER BY p.productname ASC "
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0]: required property 'data_source' not found");
    }

    @Test
    void fails_if_jdbc_data_source_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": 42,
                "query": "SELECT p.productname FROM products p ORDER BY p.productname ASC "
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0].data_source: integer found, string expected");
    }

    @Test
    void fails_if_jdbc_source_lacks_query() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "northwind"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0]: required property 'query' not found");
    }

    @Test
    void fails_if_jdbc_source_query_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                """
        {
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "data_source": "northwind",
                "query": 42
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                        .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("1 error(s)", "0 warning(s)", "$.sources[0].query: integer found, string expected");
    }
}
