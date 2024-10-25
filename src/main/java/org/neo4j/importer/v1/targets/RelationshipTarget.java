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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RelationshipTarget extends EntityTarget {
    private final String type;
    private final NodeMatchMode nodeMatchMode;
    private final String startNodeReference;
    private final String endNodeReference;
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
            @JsonProperty("start_node_reference") String startNodeReference,
            @JsonProperty("end_node_reference") String endNodeReference,
            @JsonProperty("properties") List<PropertyMapping> properties,
            @JsonProperty("schema") RelationshipSchema schema) {

        super(
                TargetType.RELATIONSHIP,
                active,
                name,
                source,
                dependencies,
                writeMode,
                sourceTransformations,
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
    public List<String> getKeyProperties() {
        if (schema == null) {
            return new ArrayList<>(0);
        }
        Set<String> result = schema.getKeyConstraints().stream()
                .flatMap(RelationshipTarget::propertyStream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        result.addAll(keyEquivalentProperties());
        return new ArrayList<>(result);
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

    private Set<String> keyEquivalentProperties() {
        var uniqueConstraints = schema.getUniqueConstraints();
        var existenceConstraints = schema.getExistenceConstraints();

        Set<String> result = new LinkedHashSet<>(Math.min(uniqueConstraints.size(), existenceConstraints.size()));
        Set<String> uniqueProperties = uniqueConstraints.stream()
                .flatMap(RelationshipTarget::propertyStream)
                .collect(Collectors.toSet());
        result.addAll(existenceConstraints.stream()
                .map(RelationshipExistenceConstraint::getProperty)
                .filter(uniqueProperties::contains)
                .collect(Collectors.toList()));
        return result;
    }

    private static Stream<String> propertyStream(RelationshipKeyConstraint constraint) {
        return propertyStream(constraint.getProperties());
    }

    private static Stream<String> propertyStream(RelationshipUniqueConstraint constraint) {
        return propertyStream(constraint.getProperties());
    }

    private static Stream<String> propertyStream(List<String> constraints) {
        return constraints.stream();
    }
}
