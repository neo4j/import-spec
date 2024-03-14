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
package org.neo4j.importer.v1.sources;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @Test
    void deserializes_BigQuery_source() throws Exception {
        var json =
                """
                {
                    "name": "my-bigquery-source",
                    "type": "bigquery",
                    "query": "SELECT id, name FROM my.table"
                }
                """
                        .stripIndent();

        var source = mapper.readValue(json, BigQuerySource.class);

        assertThat(source.getName()).isEqualTo("my-bigquery-source");
        assertThat(source.getType()).isEqualTo(SourceType.BIGQUERY);
        assertThat(source.getQuery()).isEqualTo("SELECT id, name FROM my.table");
    }

    @Test
    void deserializes_minimal_external_text_source() throws Exception {
        var json =
                """
                {
                    "name": "my-minimal-external-text-source",
                    "type": "text",
                    "urls": [
                        "https://example.com/file.csv",
                        "https://example.com/chunks/file-*.csv"
                    ]
                }
                """
                        .stripIndent();

        var source = mapper.readValue(json, ExternalTextSource.class);

        assertThat(source.getName()).isEqualTo("my-minimal-external-text-source");
        assertThat(source.getType()).isEqualTo(SourceType.TEXT);
        assertThat(source.getColumnDelimiter()).isEqualTo(",");
        assertThat(source.getLineSeparator()).isEqualTo("\n");
        assertThat(source.getUrls())
                .isEqualTo(List.of("https://example.com/file.csv", "https://example.com/chunks/file-*.csv"));
    }

    @Test
    void deserializes_external_text_source() throws Exception {
        var json =
                """
                {
                    "name": "my-external-text-source",
                    "type": "text",
                    "header": ["column_1", "column_2", "column_3"],
                    "format": "EXCEL",
                    "column_delimiter": "$",
                    "line_separator": "#",
                    "urls": [
                        "https://example.com/file.csv",
                        "https://example.com/chunks/file-*.csv"
                    ]
                }
                """
                        .stripIndent();

        var source = mapper.readValue(json, ExternalTextSource.class);

        assertThat(source.getName()).isEqualTo("my-external-text-source");
        assertThat(source.getType()).isEqualTo(SourceType.TEXT);
        assertThat(source.getHeader()).isEqualTo(List.of("column_1", "column_2", "column_3"));
        assertThat(source.getFormat()).isEqualTo(TextFormat.EXCEL);
        assertThat(source.getColumnDelimiter()).isEqualTo("$");
        assertThat(source.getLineSeparator()).isEqualTo("#");
        assertThat(source.getUrls())
                .isEqualTo(List.of("https://example.com/file.csv", "https://example.com/chunks/file-*.csv"));
    }

    @Test
    void deserializes_inline_text_source() throws Exception {
        var json =
                """
                {
                    "name": "my-external-text-source",
                    "type": "text",
                    "header": ["column_1", "column_2", "column_3"],
                    "data": [
                        ["one", 2.0, 3],
                        [1.0, 2, "three"]
                    ]
                }
                """
                        .stripIndent();

        var source = mapper.readValue(json, InlineTextSource.class);

        assertThat(source.getName()).isEqualTo("my-external-text-source");
        assertThat(source.getType()).isEqualTo(SourceType.TEXT);
        assertThat(source.getHeader()).isEqualTo(List.of("column_1", "column_2", "column_3"));
        assertThat(source.getData()).isEqualTo(List.of(List.of("one", 2.0D, 3), List.of(1.0D, 2, "three")));
    }
}
