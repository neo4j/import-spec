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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.WriteMode;

public class NodeTargetStep extends EntityTargetStep {

    private final NodeTarget target;

    NodeTargetStep(NodeTarget target, Set<ImportStep> dependencies) {
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
        return List.copyOf(distinctKeyProperties(target.getProperties(), schema()));
    }

    public NodeSchema schema() {
        return target.getSchema();
    }

    @Override
    protected NodeTarget target() {
        return target;
    }

    private static Set<PropertyMapping> distinctKeyProperties(List<PropertyMapping> properties, NodeSchema schema) {
        var mappings = indexByPropertyName(properties);
        var keyConstraints = schema.getKeyConstraints();
        if (!keyConstraints.isEmpty()) {
            return keyConstraints.stream()
                    .flatMap(constraint -> constraint.getProperties().stream())
                    .map(mappings::get)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return schema.getUniqueConstraints().stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .map(mappings::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeTargetStep)) return false;
        if (!super.equals(o)) return false;
        NodeTargetStep step = (NodeTargetStep) o;
        return Objects.equals(target, step.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), target);
    }

    @Override
    public String toString() {
        return "NodeTargetStep{" + "target=" + target + "} " + super.toString();
    }
}
