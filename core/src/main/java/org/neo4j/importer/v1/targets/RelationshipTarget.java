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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;

public class RelationshipTarget extends EntityTarget {
    private final String type;
    private final NodeMatchMode nodeMatchMode;
    private final NodeReference startNodeReference;
    private final NodeReference endNodeReference;
    private final RelationshipSchema schema;

    @JsonCreator
    public RelationshipTarget(
            @JsonProperty(value = "active", defaultValue = DEFAULT_ACTIVE) Boolean active,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty("depends_on") List<String> dependencies,
            @JsonProperty(value = "type", required = true) String type,
            @JsonProperty("write_mode") WriteMode writeMode,
            @JsonProperty("node_match_mode") NodeMatchMode nodeMatchMode,
            @JsonAnySetter ObjectNode rawExtensionData,
            @JsonProperty("start_node_reference") NodeReference startNodeReference,
            @JsonProperty("end_node_reference") NodeReference endNodeReference,
            @JsonProperty("properties") List<PropertyMapping> properties,
            @JsonProperty("schema") RelationshipSchema schema) {

        super(
                TargetType.RELATIONSHIP,
                active,
                name,
                source,
                dependencies,
                writeMode,
                mapExtensions(rawExtensionData),
                properties);
        this.type = type;
        this.nodeMatchMode = nodeMatchMode;
        this.startNodeReference = startNodeReference;
        this.endNodeReference = endNodeReference;
        this.schema = schema;
    }

    public String getType() {
        return type;
    }

    public NodeMatchMode getNodeMatchMode() {
        return nodeMatchMode;
    }

    public RelationshipSchema getSchema() {
        return schema == null ? RelationshipSchema.EMPTY : schema;
    }

    public NodeReference getStartNodeReference() {
        return startNodeReference;
    }

    public NodeReference getEndNodeReference() {
        return endNodeReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RelationshipTarget that = (RelationshipTarget) o;
        return Objects.equals(type, that.type)
                && nodeMatchMode == that.nodeMatchMode
                && Objects.equals(startNodeReference, that.startNodeReference)
                && Objects.equals(endNodeReference, that.endNodeReference)
                && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, nodeMatchMode, startNodeReference, endNodeReference, schema);
    }

    @Override
    public String toString() {
        return "RelationshipTarget{" + "type='"
                + type + '\'' + ", nodeMatchMode="
                + nodeMatchMode + ", startNodeReference='"
                + startNodeReference + '\'' + ", endNodeReference='"
                + endNodeReference + '\'' + ", schema="
                + schema + "} "
                + super.toString();
    }
}
