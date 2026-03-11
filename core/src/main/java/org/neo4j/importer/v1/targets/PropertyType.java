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
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.Objects;

public class PropertyType implements Serializable {

    public static final PropertyType BOOLEAN = new PropertyType(PropertyTypeName.BOOLEAN);
    public static final PropertyType BOOLEAN_ARRAY = new PropertyType(PropertyTypeName.BOOLEAN_ARRAY);
    public static final PropertyType BYTE_ARRAY = new PropertyType(PropertyTypeName.BYTE_ARRAY);
    public static final PropertyType DATE = new PropertyType(PropertyTypeName.DATE);
    public static final PropertyType DATE_ARRAY = new PropertyType(PropertyTypeName.DATE_ARRAY);
    public static final PropertyType DURATION = new PropertyType(PropertyTypeName.DURATION);
    public static final PropertyType DURATION_ARRAY = new PropertyType(PropertyTypeName.DURATION_ARRAY);
    public static final PropertyType FLOAT = new PropertyType(PropertyTypeName.FLOAT);
    public static final PropertyType FLOAT_ARRAY = new PropertyType(PropertyTypeName.FLOAT_ARRAY);
    public static final PropertyType INTEGER = new PropertyType(PropertyTypeName.INTEGER);
    public static final PropertyType INTEGER_ARRAY = new PropertyType(PropertyTypeName.INTEGER_ARRAY);
    public static final PropertyType LOCAL_DATETIME = new PropertyType(PropertyTypeName.LOCAL_DATETIME);
    public static final PropertyType LOCAL_DATETIME_ARRAY = new PropertyType(PropertyTypeName.LOCAL_DATETIME_ARRAY);
    public static final PropertyType LOCAL_TIME = new PropertyType(PropertyTypeName.LOCAL_TIME);
    public static final PropertyType LOCAL_TIME_ARRAY = new PropertyType(PropertyTypeName.LOCAL_TIME_ARRAY);
    public static final PropertyType POINT = new PropertyType(PropertyTypeName.POINT);
    public static final PropertyType POINT_ARRAY = new PropertyType(PropertyTypeName.POINT_ARRAY);
    public static final PropertyType STRING = new PropertyType(PropertyTypeName.STRING);
    public static final PropertyType STRING_ARRAY = new PropertyType(PropertyTypeName.STRING_ARRAY);
    public static final PropertyType ZONED_DATETIME = new PropertyType(PropertyTypeName.ZONED_DATETIME);
    public static final PropertyType ZONED_DATETIME_ARRAY = new PropertyType(PropertyTypeName.ZONED_DATETIME_ARRAY);
    public static final PropertyType ZONED_TIME = new PropertyType(PropertyTypeName.ZONED_TIME);
    public static final PropertyType ZONED_TIME_ARRAY = new PropertyType(PropertyTypeName.ZONED_TIME_ARRAY);

    private final PropertyTypeName name;
    private final Integer dimension;

    public PropertyType(PropertyTypeName name) {
        this(name, null);
    }

    @JsonCreator
    public PropertyType(@JsonProperty("name") PropertyTypeName name, @JsonProperty("dimension") Integer dimension) {
        this.name = name;
        this.dimension = dimension;
    }

    @JsonCreator
    public static PropertyType fromString(String value) {
        return new PropertyType(PropertyTypeName.valueOf(value.toUpperCase()));
    }

    @JsonProperty("name")
    public PropertyTypeName getName() {
        return name;
    }

    @JsonProperty("dimension")
    public Integer getDimension() {
        return dimension;
    }

    @JsonValue
    public Object jsonValue() {
        if (dimension == null) {
            return name;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyType that = (PropertyType) o;
        return name == that.name && Objects.equals(dimension, that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dimension);
    }

    @Override
    public String toString() {
        return "PropertyType{" + "name=" + name + ", dimension=" + dimension + '}';
    }
}
