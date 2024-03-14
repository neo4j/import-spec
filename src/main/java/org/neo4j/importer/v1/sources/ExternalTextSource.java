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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public class ExternalTextSource extends TextSource {
    private static final String DEFAULT_COLUMN_DELIMITER = ",";
    private static final String DEFAULT_LINE_SEPARATOR = "\n";
    private final List<String> urls;
    private final TextFormat format;
    private final String columnDelimiter;
    private final String lineSeparator;

    @JsonCreator
    public ExternalTextSource(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "urls", required = true) List<String> urls,
            @JsonProperty("header") List<String> header,
            @JsonProperty("format") TextFormat format,
            @JsonProperty(value = "column_delimiter", defaultValue = DEFAULT_COLUMN_DELIMITER) String columnDelimiter,
            @JsonProperty(value = "line_separator", defaultValue = DEFAULT_LINE_SEPARATOR) String lineSeparator) {
        super(name, header);
        this.urls = urls;
        this.format = format;
        this.columnDelimiter = columnDelimiter != null ? columnDelimiter : DEFAULT_COLUMN_DELIMITER;
        this.lineSeparator = lineSeparator != null ? lineSeparator : DEFAULT_LINE_SEPARATOR;
    }

    public List<String> getUrls() {
        return urls;
    }

    public TextFormat getFormat() {
        return format;
    }

    public String getColumnDelimiter() {
        return columnDelimiter;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExternalTextSource that = (ExternalTextSource) o;
        return Objects.equals(urls, that.urls)
                && format == that.format
                && Objects.equals(columnDelimiter, that.columnDelimiter)
                && Objects.equals(lineSeparator, that.lineSeparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), urls, format, columnDelimiter, lineSeparator);
    }

    @Override
    public String toString() {
        return "ExternalTextSource{" + "urls="
                + urls + ", format="
                + format + ", columnDelimiter='"
                + columnDelimiter + '\'' + ", lineSeparator='"
                + lineSeparator + '\'' + "} "
                + super.toString();
    }
}
