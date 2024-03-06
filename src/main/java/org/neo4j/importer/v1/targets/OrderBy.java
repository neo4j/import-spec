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
package org.neo4j.importer.v1.targets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

public class OrderBy implements Serializable {

    private final String expression;
    private final Order order;
    // TODO: collate?

    @JsonCreator
    public OrderBy(
            @JsonProperty(value = "expression", required = true) String expression,
            @JsonProperty("order") Order order) {

        this.expression = expression;
        this.order = order;
    }

    public String getExpression() {
        return expression;
    }

    public Order getOrder() {
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderBy orderBy = (OrderBy) o;
        return Objects.equals(expression, orderBy.expression) && order == orderBy.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, order);
    }

    @Override
    public String toString() {
        return "OrderBy{" + "expression='" + expression + '\'' + ", order=" + order + '}';
    }
}
