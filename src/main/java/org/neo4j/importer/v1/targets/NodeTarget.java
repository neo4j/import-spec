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
import java.util.List;
import java.util.Objects;

public class NodeTarget extends Target {
    private final WriteMode writeMode;
    private final SourceTransformations sourceTransformations;
    private final List<String> labels;
    private final List<PropertyMapping> properties;
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
        super(active, name, source, dependencies);
        this.writeMode = writeMode;
        this.sourceTransformations = sourceTransformations;
        this.labels = labels;
        this.properties = properties;
        this.schema = schema;
    }

    public WriteMode getWriteMode() {
        return writeMode;
    }

    public SourceTransformations getSourceTransformations() {
        return sourceTransformations;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<PropertyMapping> getProperties() {
        return properties;
    }

    public NodeSchema getSchema() {
        return schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NodeTarget that = (NodeTarget) o;
        return writeMode == that.writeMode
                && Objects.equals(sourceTransformations, that.sourceTransformations)
                && Objects.equals(labels, that.labels)
                && Objects.equals(properties, that.properties)
                && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), writeMode, sourceTransformations, labels, properties, schema);
    }

    @Override
    public String toString() {
        return "NodeTarget{" + "writeMode="
                + writeMode + ", sourceTransformations="
                + sourceTransformations + ", labels="
                + labels + ", properties="
                + properties + ", schema="
                + schema + "} "
                + super.toString();
    }
}
