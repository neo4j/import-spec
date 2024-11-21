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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Target implements Comparable<Target>, Serializable {

    protected static final String DEFAULT_ACTIVE = "true";
    private final TargetType targetType;
    private final boolean active;
    private final String name;
    private final String source;
    private final List<String> dependencies;

    Target(TargetType targetType, Boolean active, String name, String source, List<String> dependencies) {
        this.targetType = targetType;
        this.active = active != null ? active : Boolean.valueOf(DEFAULT_ACTIVE).booleanValue();
        this.name = name;
        this.source = source;
        this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public boolean isActive() {
        return active;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    @Override
    public int compareTo(Target other) {
        if (other.dependsOn(this)) {
            return -1;
        }
        if (this.dependsOn(other)) {
            return 1;
        }
        return this.getName().compareTo(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Target target = (Target) o;
        return active == target.active
                && targetType == target.targetType
                && Objects.equals(name, target.name)
                && Objects.equals(source, target.source)
                && Objects.equals(dependencies, target.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetType, active, name, source, dependencies);
    }

    @Override
    public String toString() {
        return "Target{" + "targetType="
                + targetType + ", active="
                + active + ", name='"
                + name + '\'' + ", source='"
                + source + '\'' + ", dependencies="
                + dependencies + '}';
    }

    protected boolean dependsOn(Target target) {
        return getDependencies().contains(target.getName());
    }
}
