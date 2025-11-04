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

import java.util.Objects;

public class JdbcSource implements Source {

    private final String name;
    private final String dataSource;
    private final String sql;

    public JdbcSource(String name, String dataSource, String sql) {
        this.name = name;
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
    public String getType() {
        return "jdbc";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JdbcSource that)) return false;
        return Objects.equals(name, that.name)
                && Objects.equals(dataSource, that.dataSource)
                && Objects.equals(sql, that.sql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataSource, sql);
    }

    @Override
    public String toString() {
        return "JdbcSource{" + "name='"
                + name + '\'' + ", dataSource='"
                + dataSource + '\'' + ", sql='"
                + sql + '\'' + '}';
    }
}
