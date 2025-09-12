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
package org.neo4j.importer.v1.pipeline;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.NodeMatchMode;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipExistenceConstraint;
import org.neo4j.importer.v1.targets.RelationshipSchema;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.RelationshipUniqueConstraint;
import org.neo4j.importer.v1.targets.WriteMode;

public class RelationshipTargetStep extends EntityTargetStep {

    private final RelationshipTarget target;
    private final NodeTargetStep startNode;
    private final NodeTargetStep endNode;

    RelationshipTargetStep(
            RelationshipTarget target, NodeTargetStep startNode, NodeTargetStep endNode, Set<ImportStep> dependencies) {
        super(dependencies);
        this.target = target;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public WriteMode writeMode() {
        return target.getWriteMode();
    }

    public NodeMatchMode nodeMatchMode() {
        return target.getNodeMatchMode();
    }

    public String type() {
        return target.getType();
    }

    @Override
    public List<PropertyMapping> keyProperties() {
        return List.copyOf(distinctKeyProperties(target.getProperties(), target.getSchema()));
    }

    @Override
    public List<PropertyMapping> nonKeyProperties() {
        var schema = target.getSchema();
        var mappings = target.getProperties();
        if (schema.isEmpty()) {
            return mappings;
        }
        var keyProperties = distinctKeyProperties(mappings, schema);
        return mappings.stream()
                .filter(mapping -> !keyProperties.contains(mapping))
                .collect(Collectors.toUnmodifiableList());
    }

    public NodeTargetStep startNode() {
        return startNode;
    }

    public NodeTargetStep endNode() {
        return endNode;
    }

    public RelationshipSchema schema() {
        return target.getSchema();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RelationshipTargetStep)) return false;
        if (!super.equals(o)) return false;
        RelationshipTargetStep that = (RelationshipTargetStep) o;
        return Objects.equals(target, that.target)
                && Objects.equals(startNode, that.startNode)
                && Objects.equals(endNode, that.endNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), target, startNode, endNode);
    }

    @Override
    public String toString() {
        return "RelationshipTargetStep{" + "target="
                + target + ", startNode="
                + startNode + ", endNode="
                + endNode + "} "
                + super.toString();
    }

    @Override
    protected RelationshipTarget target() {
        return target;
    }

    private static Set<PropertyMapping> distinctKeyProperties(
            List<PropertyMapping> properties, RelationshipSchema schema) {
        var mappings = indexByPropertyName(properties);
        Set<PropertyMapping> result = schema.getKeyConstraints().stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .map(mappings::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        result.addAll(
                keyEquivalentProperties(schema.getUniqueConstraints(), schema.getExistenceConstraints(), mappings));
        return result;
    }

    private static Map<String, PropertyMapping> indexByPropertyName(List<PropertyMapping> mappings) {
        return mappings.stream().collect(Collectors.toMap(PropertyMapping::getTargetProperty, Function.identity()));
    }

    private static Set<PropertyMapping> keyEquivalentProperties(
            List<RelationshipUniqueConstraint> uniqueConstraints,
            List<RelationshipExistenceConstraint> existenceConstraints,
            Map<String, PropertyMapping> mappings) {

        Set<PropertyMapping> result =
                new LinkedHashSet<>(Math.min(uniqueConstraints.size(), existenceConstraints.size()));
        Set<PropertyMapping> uniqueProperties = uniqueConstraints.stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .map(mappings::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        result.addAll(existenceConstraints.stream()
                .map(constraint -> mappings.get(constraint.getProperty()))
                .filter(uniqueProperties::contains)
                .collect(Collectors.toList()));
        return result;
    }
}
