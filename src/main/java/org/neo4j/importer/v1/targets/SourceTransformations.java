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
package org.neo4j.importer.v1.targets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class SourceTransformations implements Serializable {
    private final boolean enableGrouping;
    private final List<Aggregation> aggregations;
    private final String whereClause;
    private final List<OrderBy> orderByClauses;
    private final Integer limit;

    @JsonCreator
    public SourceTransformations(
            @JsonProperty("enable_grouping") boolean enableGrouping,
            @JsonProperty("aggregations") List<Aggregation> aggregations,
            @JsonProperty("where") String whereClause,
            @JsonProperty("order_by") List<OrderBy> orderByClauses,
            @JsonProperty("limit") Integer limit) {

        this.enableGrouping = enableGrouping;
        this.aggregations = aggregations;
        this.whereClause = whereClause;
        this.orderByClauses = orderByClauses;
        this.limit = limit;
    }

    public boolean isEnableGrouping() {
        return enableGrouping;
    }

    public List<Aggregation> getAggregations() {
        return aggregations;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public List<OrderBy> getOrderByClauses() {
        return orderByClauses;
    }

    public Integer getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceTransformations that = (SourceTransformations) o;
        return enableGrouping == that.enableGrouping
                && Objects.equals(aggregations, that.aggregations)
                && Objects.equals(whereClause, that.whereClause)
                && Objects.equals(orderByClauses, that.orderByClauses)
                && Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableGrouping, aggregations, whereClause, orderByClauses, limit);
    }

    @Override
    public String toString() {
        return "SourceTransformations{" + "enableGrouping="
                + enableGrouping + ", aggregations="
                + aggregations + ", whereClause='"
                + whereClause + '\'' + ", orderByClauses="
                + orderByClauses + ", limit="
                + limit + '}';
    }
}
