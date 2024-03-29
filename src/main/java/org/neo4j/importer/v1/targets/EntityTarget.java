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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class EntityTarget extends Target {
    private final WriteMode writeMode;
    private final SourceTransformations sourceTransformations;
    private final List<PropertyMapping> properties;

    public EntityTarget(
            TargetType targetType,
            Boolean active,
            String name,
            String source,
            List<String> dependencies,
            WriteMode writeMode,
            SourceTransformations sourceTransformations,
            List<PropertyMapping> properties) {
        super(targetType, active, name, source, dependencies);
        this.writeMode = writeMode;
        this.sourceTransformations = sourceTransformations;
        this.properties = properties;
    }

    public WriteMode getWriteMode() {
        return writeMode;
    }

    public SourceTransformations getSourceTransformations() {
        return sourceTransformations;
    }

    public List<PropertyMapping> getProperties() {
        // properties can be null for relationship targets
        return properties != null ? properties : Collections.emptyList();
    }

    /**
     * getKeyProperties returns the list of properties part of key constraints, or part of both unique and existence
     * constraints.
     * These are typically used when defining a node/relationship MERGE pattern.
     * Call {@link NodeTarget#getSchema()} or {@link RelationshipTarget#getSchema()} to get properties that are only
     * part of key constraints.
     */
    public abstract List<String> getKeyProperties();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityTarget)) return false;
        if (!super.equals(o)) return false;
        EntityTarget that = (EntityTarget) o;
        return writeMode == that.writeMode
                && Objects.equals(sourceTransformations, that.sourceTransformations)
                && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), writeMode, sourceTransformations, properties);
    }

    @Override
    public String toString() {
        return "EntityTarget{" + "writeMode="
                + writeMode + ", sourceTransformations="
                + sourceTransformations + ", properties="
                + properties + "} "
                + super.toString();
    }
}
