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

import java.util.List;
import java.util.Objects;
import org.neo4j.importer.v1.targets.Target;

public abstract class TargetStep implements ImportStep {

    private final List<ImportStep> dependencies;

    protected TargetStep(List<ImportStep> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String name() {
        return target().getName();
    }

    public String sourceName() {
        return target().getSource();
    }

    public List<ImportStep> dependencies() {
        return dependencies != null ? dependencies : List.of();
    }

    protected abstract Target target();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TargetStep)) return false;
        TargetStep that = (TargetStep) o;
        return Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dependencies);
    }
}
