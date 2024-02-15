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

import java.util.Objects;

public abstract class Target {

    protected static final String DEFAULT_ACTIVE = "true";
    private final boolean active;
    private final String name;
    private final String source;
    private final String dependsOn;

    Target(Boolean active, String name, String source, String dependsOn) {
        this.active = active != null ? active : Boolean.valueOf(DEFAULT_ACTIVE).booleanValue();
        this.name = name;
        this.source = source;
        this.dependsOn = dependsOn;
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

    public String getDependsOn() {
        return dependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Target target = (Target) o;
        return active == target.active
                && Objects.equals(name, target.name)
                && Objects.equals(source, target.source)
                && Objects.equals(dependsOn, target.dependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, name, source, dependsOn);
    }

    @Override
    public String toString() {
        return "Target{" + "active="
                + active + ", name='"
                + name + '\'' + ", source='"
                + source + '\'' + ", dependsOn='"
                + dependsOn + '\'' + '}';
    }
}
