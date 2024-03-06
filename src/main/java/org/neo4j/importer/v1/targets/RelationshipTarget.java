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

public class RelationshipTarget extends Target {
    private final String type;
    private final WriteMode writeMode;
    private final NodeMatchMode nodeMatchMode;
    private final SourceTransformations sourceTransformations;
    private final RelationshipNode startNode;
    private final String startNodeReference;
    private final RelationshipNode endNode;
    private final String endNodeReference;
    private final List<PropertyMapping> properties;
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
            @JsonProperty("source_transformations") SourceTransformations sourceTransformations,
            @JsonProperty("start_node") RelationshipNode startNode,
            @JsonProperty("start_node_reference") String startNodeReference,
            @JsonProperty("end_node") RelationshipNode endNode,
            @JsonProperty("end_node_reference") String endNodeReference,
            @JsonProperty("properties") List<PropertyMapping> properties,
            @JsonProperty("schema") RelationshipSchema schema) {

        super(active, name, source, dependencies);
        this.type = type;
        this.writeMode = writeMode;
        this.nodeMatchMode = nodeMatchMode;
        this.sourceTransformations = sourceTransformations;
        this.startNode = startNode;
        this.startNodeReference = startNodeReference;
        this.endNode = endNode;
        this.endNodeReference = endNodeReference;
        this.properties = properties;
        this.schema = schema;
    }

    public String getType() {
        return type;
    }

    public WriteMode getWriteMode() {
        return writeMode;
    }

    public NodeMatchMode getNodeMatchMode() {
        return nodeMatchMode;
    }

    public SourceTransformations getSourceTransformations() {
        return sourceTransformations;
    }

    public RelationshipNode getStartNode() {
        return startNode;
    }

    public RelationshipNode getEndNode() {
        return endNode;
    }

    public List<PropertyMapping> getProperties() {
        return properties == null ? Collections.emptyList() : properties;
    }

    public RelationshipSchema getSchema() {
        return schema;
    }

    public String getStartNodeReference() {
        return startNodeReference;
    }

    public String getEndNodeReference() {
        return endNodeReference;
    }

    @Override
    public boolean dependsOn(Target target) {
        if (super.dependsOn(target)) {
            return true;
        }
        if (!(target instanceof NodeTarget)) {
            return false;
        }
        String nodeTargetName = target.getName();
        return nodeTargetName.equals(startNodeReference) || nodeTargetName.equals(endNodeReference);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RelationshipTarget that = (RelationshipTarget) o;
        return Objects.equals(type, that.type)
                && writeMode == that.writeMode
                && nodeMatchMode == that.nodeMatchMode
                && Objects.equals(sourceTransformations, that.sourceTransformations)
                && Objects.equals(startNode, that.startNode)
                && Objects.equals(startNodeReference, that.startNodeReference)
                && Objects.equals(endNode, that.endNode)
                && Objects.equals(endNodeReference, that.endNodeReference)
                && Objects.equals(properties, that.properties)
                && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                type,
                writeMode,
                nodeMatchMode,
                sourceTransformations,
                startNode,
                startNodeReference,
                endNode,
                endNodeReference,
                properties,
                schema);
    }

    @Override
    public String toString() {
        return "RelationshipTarget{" + "type='"
                + type + '\'' + ", writeMode="
                + writeMode + ", nodeMatchMode="
                + nodeMatchMode + ", sourceTransformations="
                + sourceTransformations + ", startNode="
                + startNode + ", startNodeReference='"
                + startNodeReference + '\'' + ", endNode="
                + endNode + ", endNodeReference='"
                + endNodeReference + '\'' + ", properties="
                + properties + ", schema="
                + schema + "} "
                + super.toString();
    }
}
