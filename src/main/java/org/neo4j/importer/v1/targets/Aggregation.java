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
import java.util.Objects;

public class Aggregation implements Serializable {

    private final String expression;
    private final String fieldName;

    @JsonCreator
    public Aggregation(
            @JsonProperty(value = "expression", required = true) String expression,
            @JsonProperty(value = "field_name", required = true) String fieldName) {

        this.expression = expression;
        this.fieldName = fieldName;
    }

    public String getExpression() {
        return expression;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Aggregation that = (Aggregation) o;
        return Objects.equals(expression, that.expression) && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, fieldName);
    }

    @Override
    public String toString() {
        return "Aggregation{" + "expression='" + expression + '\'' + ", fieldName='" + fieldName + '\'' + '}';
    }
}
