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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SourceTransformationsProvider implements EntityTargetExtensionProvider<SourceTransformations> {

    @Override
    public SourceTransformations apply(ObjectNode node) {
        if (!node.has("source_transformations")) {
            return null;
        }
        var transformations = node.get("source_transformations");
        // note: real implementation would need to take proper care of optional attributes
        return new SourceTransformations(
                transformations.get("enable_grouping").asBoolean(false),
                mapAggregations(transformations.get("aggregations")),
                transformations.get("where").textValue(),
                mapOrderBys(transformations.get("order_by")),
                transformations.get("limit").asInt());
    }

    private List<Aggregation> mapAggregations(JsonNode aggregations) {
        if (!aggregations.isArray()) {
            throw new IllegalArgumentException("aggregations must be an array");
        }
        var result = new ArrayList<Aggregation>();
        aggregations.forEach(aggregation -> {
            result.add(new Aggregation(
                    aggregation.get("expression").textValue(),
                    aggregation.get("field_name").textValue()));
        });
        return result;
    }

    private List<OrderBy> mapOrderBys(JsonNode orderBys) {
        if (!orderBys.isArray()) {
            throw new IllegalArgumentException("order_by must be an array");
        }
        var result = new ArrayList<OrderBy>();
        orderBys.forEach(orderBy -> {
            result.add(new OrderBy(
                    orderBy.get("expression").textValue(),
                    !orderBy.has("order")
                            ? null
                            : Order.valueOf(orderBy.get("order").textValue().toUpperCase(Locale.ROOT))));
        });
        return result;
    }
}
