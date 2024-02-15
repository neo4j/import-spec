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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Targets {

    private final List<NodeTarget> nodes;
    private final List<RelationshipTarget> relationships;
    private final List<CustomQueryTarget> customQueries;

    @JsonCreator
    public Targets(
            @JsonProperty("nodes") List<NodeTarget> nodes,
            @JsonProperty("relationships") List<RelationshipTarget> relationships,
            @JsonProperty("queries") List<CustomQueryTarget> customQueries) {

        this.nodes = nodes;
        this.relationships = relationships;
        this.customQueries = customQueries;
    }

    public List<NodeTarget> getNodes() {
        return nodes == null ? Collections.emptyList() : nodes;
    }

    public List<RelationshipTarget> getRelationships() {
        return relationships == null ? Collections.emptyList() : relationships;
    }

    public List<CustomQueryTarget> getCustomQueries() {
        return customQueries == null ? Collections.emptyList() : customQueries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Targets targets = (Targets) o;
        return Objects.equals(nodes, targets.nodes)
                && Objects.equals(relationships, targets.relationships)
                && Objects.equals(customQueries, targets.customQueries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, relationships, customQueries);
    }

    @Override
    public String toString() {
        return "Targets{" + "nodes="
                + nodes + ", relationships="
                + relationships + ", customQueries="
                + customQueries + '}';
    }
}
