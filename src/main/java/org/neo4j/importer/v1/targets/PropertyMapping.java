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
import java.util.Objects;

public class PropertyMapping {

    private final String sourceField;
    private final String targetProperty;
    private final PropertyType targetPropertyType;

    @JsonCreator
    public PropertyMapping(
            @JsonProperty(value = "source_field", required = true) String sourceField,
            @JsonProperty(value = "target_property", required = true) String targetProperty,
            @JsonProperty("target_property_type") PropertyType targetPropertyType) {

        this.sourceField = sourceField;
        this.targetProperty = targetProperty;
        this.targetPropertyType = targetPropertyType;
    }

    public String getSourceField() {
        return sourceField;
    }

    public String getTargetProperty() {
        return targetProperty;
    }

    public PropertyType getTargetPropertyType() {
        return targetPropertyType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyMapping that = (PropertyMapping) o;
        return Objects.equals(sourceField, that.sourceField)
                && Objects.equals(targetProperty, that.targetProperty)
                && targetPropertyType == that.targetPropertyType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceField, targetProperty, targetPropertyType);
    }

    @Override
    public String toString() {
        return "PropertyMapping{" + "sourceField='"
                + sourceField + '\'' + ", targetProperty='"
                + targetProperty + '\'' + ", targetPropertyType="
                + targetPropertyType + '}';
    }
}
