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
package org.neo4j.importer.v1.actions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class BigQueryAction extends Action {
    private final String sql;

    @JsonCreator
    public BigQueryAction(
            @JsonProperty(value = "active", defaultValue = DEFAULT_ACTIVE) Boolean active,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "stage", required = true) ActionStage stage,
            @JsonProperty(value = "sql", required = true) String sql) {

        super(active, name, ActionType.BIGQUERY, stage);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BigQueryAction that = (BigQueryAction) o;
        return Objects.equals(sql, that.sql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sql);
    }

    @Override
    public String toString() {
        return "BigQueryAction{" + "sql='" + sql + '\'' + "} " + super.toString();
    }
}
