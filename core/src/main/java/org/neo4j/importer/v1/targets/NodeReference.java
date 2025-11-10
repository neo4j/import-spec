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

/**
 * {@link NodeReference} defines a named reference to a start/end {@link NodeTarget} of a {@link RelationshipTarget}.<br>
 * The corresponding {@link NodeTarget} is found by the provided name.<br>
 * The node lookup pattern is determined either:
 * <ol>
 *     <li>by looking original field names of the {@link NodeTarget} key properties.
 *     </li>
 *     <li>by key mapping overrides: {@link KeyMapping} controls the field and property names to use for the lookup</li>
 * </ol>
 */
public class NodeReference implements Serializable {
    private final String name;
    private final List<KeyMapping> keyMappings;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public NodeReference(String name) {
        this(name, null);
    }

    @JsonCreator
    public NodeReference(
            @JsonProperty("name") String name, @JsonProperty("key_mappings") List<KeyMapping> keyMappings) {
        this.name = name;
        this.keyMappings = keyMappings;
    }

    /**
     * Returns the name of the {@link NodeTarget}
     * @return node target name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the node key mapping overrides
     * @return the node key mapping overrides, this is never <code>null</code>
     */
    public List<KeyMapping> getKeyMappings() {
        return keyMappings != null ? keyMappings : List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeReference)) return false;
        NodeReference that = (NodeReference) o;
        return Objects.equals(name, that.name) && Objects.equals(keyMappings, that.keyMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, keyMappings);
    }

    @Override
    public String toString() {
        return "NodeReference{" + "name='" + name + '\'' + ", keyMappings=" + keyMappings + '}';
    }
}
