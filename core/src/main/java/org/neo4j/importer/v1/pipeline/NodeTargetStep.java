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
import org.neo4j.importer.v1.targets.NodeExistenceConstraint;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeUniqueConstraint;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.WriteMode;

public class NodeTargetStep extends EntityTargetStep {

    private final NodeTarget target;

    NodeTargetStep(NodeTarget target, List<ImportStep> dependencies) {
        super(dependencies);
        this.target = target;
    }

    public WriteMode writeMode() {
        return target.getWriteMode();
    }

    public List<String> labels() {
        return target.getLabels();
    }

    @Override
    public List<PropertyMapping> keyProperties() {
        var schema = schema();
        if (schema == null) {
            return List.of();
        }
        return List.copyOf(distinctKeyProperties(target.getProperties(), schema));
    }

    @Override
    public List<PropertyMapping> nonKeyProperties() {
        var schema = schema();
        var mappings = target.getProperties();
        if (schema == null) {
            return mappings;
        }
        var keyProperties = distinctKeyProperties(mappings, schema);
        return mappings.stream()
                .filter(mapping -> !keyProperties.contains(mapping))
                .collect(Collectors.toUnmodifiableList());
    }

    public NodeSchema schema() {
        return target.getSchema();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeTargetStep)) return false;
        if (!super.equals(o)) return false;
        NodeTargetStep task = (NodeTargetStep) o;
        return Objects.equals(target, task.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), target);
    }

    @Override
    protected NodeTarget target() {
        return target;
    }

    private static Set<PropertyMapping> distinctKeyProperties(List<PropertyMapping> properties, NodeSchema schema) {
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
            List<NodeUniqueConstraint> uniqueConstraints,
            List<NodeExistenceConstraint> existenceConstraints,
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
