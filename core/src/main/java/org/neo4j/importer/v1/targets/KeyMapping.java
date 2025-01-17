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

public class KeyMapping {

    private final String sourceField;

    private final String nodeProperty;

    @JsonCreator
    public KeyMapping(
            @JsonProperty("source_field") String sourceField, @JsonProperty("node_property") String nodeProperty) {
        this.sourceField = sourceField;
        this.nodeProperty = nodeProperty;
    }

    public String getSourceField() {
        return sourceField;
    }

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
