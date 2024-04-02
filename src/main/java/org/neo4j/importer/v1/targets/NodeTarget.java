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

public class NodeTarget extends EntityTarget {
    private final List<String> labels;
    private final NodeSchema schema;

    @JsonCreator
    public NodeTarget(
            @JsonProperty(value = "active", defaultValue = DEFAULT_ACTIVE) Boolean active,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty("depends_on") List<String> dependencies,
            @JsonProperty("write_mode") WriteMode writeMode,
            @JsonProperty("source_transformations") SourceTransformations sourceTransformations,
            @JsonProperty(value = "labels", required = true) List<String> labels,
            @JsonProperty(value = "properties", required = true) List<PropertyMapping> properties,
            @JsonProperty("schema") NodeSchema schema) {

        super(TargetType.NODE, active, name, source, dependencies, writeMode, sourceTransformations, properties);
        this.labels = labels;
        this.schema = schema;
    }

    public List<String> getLabels() {
        return labels;
    }

    public NodeSchema getSchema() {
        return schema;
    }

    @Override
    public List<String> getKeyProperties() {
        Set<String> result = schema.getKeyConstraints().stream()
                .flatMap(NodeTarget::propertyStream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        result.addAll(keyEquivalentProperties());
        return new ArrayList<>(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NodeTarget that = (NodeTarget) o;
        return Objects.equals(labels, that.labels) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), labels, schema);
    }

    @Override
    public String toString() {
        return "NodeTarget{" + "labels=" + labels + ", schema=" + schema + "} " + super.toString();
    }

    private Set<String> keyEquivalentProperties() {
        var uniqueConstraints = schema.getUniqueConstraints();
        var existenceConstraints = schema.getExistenceConstraints();

        Set<String> result = new LinkedHashSet<>(Math.min(uniqueConstraints.size(), existenceConstraints.size()));
        Set<String> uniqueProperties =
                uniqueConstraints.stream().flatMap(NodeTarget::propertyStream).collect(Collectors.toSet());
        result.addAll(existenceConstraints.stream()
                .map(NodeExistenceConstraint::getProperty)
                .filter(uniqueProperties::contains)
                .collect(Collectors.toList()));
        return result;
    }

    private static Stream<String> propertyStream(NodeKeyConstraint constraint) {
        return propertyStream(constraint.getProperties());
    }

    private static Stream<String> propertyStream(NodeUniqueConstraint constraint) {
        return propertyStream(constraint.getProperties());
    }

    private static Stream<String> propertyStream(List<String> constraints) {
        return constraints.stream();
    }
}
