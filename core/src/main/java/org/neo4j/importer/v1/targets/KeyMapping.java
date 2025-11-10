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
import java.util.Objects;

/**
 * {@link KeyMapping} defines the node lookup override for the given start/end node of a {@link RelationshipTarget}.
 */
public class KeyMapping {

    private final String sourceField;

    private final String nodeProperty;

    @JsonCreator
    public KeyMapping(
            @JsonProperty("source_field") String sourceField, @JsonProperty("node_property") String nodeProperty) {
        this.sourceField = sourceField;
        this.nodeProperty = nodeProperty;
    }

    /**
     * Source field name to use when mapping data from the enclosing {@link  RelationshipTarget} source
     * @return the source field
     */
    public String getSourceField() {
        return sourceField;
    }

    /**
     * Key property name to use when looking the start/end node named by the enclosing {@link NodeReference}
     * @return the source field
     */
    public String getNodeProperty() {
        return nodeProperty;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeyMapping)) return false;
        KeyMapping that = (KeyMapping) o;
        return Objects.equals(sourceField, that.sourceField) && Objects.equals(nodeProperty, that.nodeProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceField, nodeProperty);
    }

    @Override
    public String toString() {
        return "KeyMapping{" + "sourceField='" + sourceField + '\'' + ", nodeProperty='" + nodeProperty + '\'' + '}';
    }
}
