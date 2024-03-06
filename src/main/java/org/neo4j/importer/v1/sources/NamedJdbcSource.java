/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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
import java.util.Objects;

public class NamedJdbcSource extends Source {

    private final String dataSource;
    private final String sql;

    @JsonCreator
    public NamedJdbcSource(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "data_source", required = true) String dataSource,
            @JsonProperty(value = "sql", required = true) String sql) {

        super(name, SourceType.JDBC);
        this.dataSource = dataSource;
        this.sql = sql;
    }

    public String getDataSource() {
        return dataSource;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NamedJdbcSource that = (NamedJdbcSource) o;
        return Objects.equals(dataSource, that.dataSource) && Objects.equals(sql, that.sql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataSource, sql);
    }

    @Override
    public String toString() {
        return "NamedJdbcSource{" + "dataSource='"
                + dataSource + '\'' + ", query='"
                + sql + '\'' + "} "
                + super.toString();
    }
}
