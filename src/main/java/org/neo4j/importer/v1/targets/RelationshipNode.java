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
import java.util.List;
import java.util.Objects;

public class RelationshipNode implements Serializable {

    private final String label;
    private final List<PropertyMapping> keyProperties;

    @JsonCreator
    public RelationshipNode(
            @JsonProperty(value = "label", required = true) String label,
            @JsonProperty(value = "key_properties", required = true) List<PropertyMapping> keyProperties) {

        this.label = label;
        this.keyProperties = keyProperties;
    }

    public String getLabel() {
        return label;
    }

    public List<PropertyMapping> getKeyProperties() {
        return keyProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipNode that = (RelationshipNode) o;
        return Objects.equals(label, that.label) && Objects.equals(keyProperties, that.keyProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, keyProperties);
    }

    @Override
    public String toString() {
        return "RelationshipNode{" + "label='" + label + '\'' + ", keyProperties=" + keyProperties + '}';
    }
}
