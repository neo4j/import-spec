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
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.sources[0].type: does not have a value in the enumeration [bigquery, jdbc, text]");
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
                .hasMessageContainingAll(
                        "0 warning(s)",
                        "$.sources[0].type: does not have a value in the enumeration [bigquery, jdbc, text]");
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'query' not found");
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].query: integer found, string expected");
    }

    @Test
    void fails_if_jdbc_data_source_is_missing() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "sql": "SELECT p.productname FROM products p ORDER BY p.productname ASC "
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'data_source' not found");
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
                "sql": "SELECT p.productname FROM products p ORDER BY p.productname ASC "
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].data_source: integer found, string expected");
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'sql' not found");
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
                "sql": 42
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].sql: integer found, string expected");
    }

    @Test
    void fails_if_external_text_source_defines_explicit_empty_header() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": [],
                "urls": [
                    "https://example.com/my.csv"
                ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].header: must have at least 1 items but found 0");
    }

    @Test
    void fails_if_inline_text_source_defines_explicit_empty_header() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": [],
                "data": [
                    ["foo"], ["bar"]
                ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].header: must have at least 1 items but found 0");
    }

    @Test
    void fails_if_external_text_source_header_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": 42,
                "urls": [
                    "https://example.com/my.csv"
                ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].header: integer found, array expected");
    }

    @Test
    void fails_if_inline_text_source_header_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": 42,
                "data": [
                    ["foo"], ["bar"]
                ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].header: integer found, array expected");
    }

    @Test
    void fails_if_external_text_source_header_includes_empty_strings() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": [""],
                "urls": [
                    "https://example.com/my.csv"
                ]
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
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.sources[0].header[0]: must be at least 1 characters long",
                        "$.sources[0].header[0]: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_inline_text_source_header_includes_empty_strings() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": [""],
                "data": [
                    ["foo"], ["bar"]
                ]
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
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.sources[0].header[0]: must be at least 1 characters long",
                        "$.sources[0].header[0]: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_external_text_source_header_includes_blank_strings() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": ["   "],
                "urls": [
                    "https://example.com/my.csv"
                ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].header[0]: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_inline_text_source_header_includes_blank_strings() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "sources": [{
                "name": "a-source",
                "type": "text",
                "header": ["   "],
                "data": [
                    ["foo"], ["bar"]
                ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].header[0]: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_external_text_source_urls_are_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["column"],
        "urls": []
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].urls: must have at least 1 items but found 0");
    }

    @Test
    void fails_if_external_text_source_urls_include_empty_string() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["column"],
        "urls": [""]
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
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.sources[0].urls[0]: must be at least 1 characters long",
                        "$.sources[0].urls[0]: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_external_text_source_urls_include_blank_string() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["column"],
        "urls": ["   "]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].urls[0]: does not match the regex pattern \\S+");
    }

    @Test
    void fails_if_external_text_source_format_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "format": 42
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
                .hasMessageContainingAll(
                        "2 error(s)",
                        "0 warning(s)",
                        "$.sources[0].format: integer found, string expected",
                        "$.sources[0].format: does not have a value in the enumeration [default, excel, informix, mongo, mongo_tsv, mysql, oracle, postgres, postgresql_csv, rfc4180]");
    }

    @Test
    void fails_if_external_text_source_format_is_unsupported() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "format": "foobar"
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
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.sources[0].format: does not have a value in the enumeration [default, excel, informix, mongo, mongo_tsv, mysql, oracle, postgres, postgresql_csv, rfc4180]");
    }

    @Test
    void fails_if_external_text_source_column_delimiter_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "column_delimiter": 42
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].column_delimiter: integer found, string expected");
    }

    @Test
    void fails_if_external_text_source_column_delimiter_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "column_delimiter": ""
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
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.sources[0].column_delimiter: must be at least 1 characters long");
    }

    @Test
    void fails_if_external_text_source_column_delimiter_is_too_long() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "column_delimiter": "--"
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
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.sources[0].column_delimiter: must be at most 1 characters long");
    }

    @Test
    void fails_if_external_text_source_line_separator_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "line_separator": 42
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].line_separator: integer found, string expected");
    }

    @Test
    void fails_if_external_text_source_line_separator_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "line_separator": ""
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
                .hasMessageContainingAll(
                        "1 error(s)",
                        "0 warning(s)",
                        "$.sources[0].line_separator: must be at least 1 characters long");
    }

    @Test
    void fails_if_external_text_source_line_separator_is_too_long() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "urls": ["https://example.com/file.csv"],
        "line_separator": "--"
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].line_separator: must be at most 1 characters long");
    }

    @Test
    void fails_if_inline_text_source_line_lacks_header() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "data": [["data1"]]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'header' not found");
    }

    @Test
    void fails_if_inline_text_source_line_lacks_data() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["column"]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0]: required property 'data' not found");
    }

    @Test
    void fails_if_inline_text_source_line_data_is_empty() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["column"],
        "data": []
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].data: must have at least 1 items but found 0");
    }

    @Test
    void fails_if_inline_text_source_line_data_contains_empty_row() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
{
    "sources": [{
        "name": "a-source",
        "type": "text",
        "header": ["column"],
        "data": [
            []
        ]
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
                .hasMessageContainingAll(
                        "1 error(s)", "0 warning(s)", "$.sources[0].data[0]: must have at least 1 items but found 0");
    }
}
