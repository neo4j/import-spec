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

import java.util.Objects;
import java.util.Set;
import org.neo4j.importer.v1.targets.CustomQueryTarget;

public class CustomQueryTargetStep extends TargetStep {

    private final CustomQueryTarget target;

    CustomQueryTargetStep(CustomQueryTarget target, Set<ImportStep> dependencies) {
        super(dependencies);
        this.target = target;
    }

    public String query() {
        return target.getQuery();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomQueryTargetStep)) return false;
        if (!super.equals(o)) return false;
        CustomQueryTargetStep that = (CustomQueryTargetStep) o;
        return Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), target);
    }

    @Override
    public String toString() {
        return "CustomQueryTargetStep{" + "target=" + target + "} " + super.toString();
    }

    @Override
    protected CustomQueryTarget target() {
        return target;
    }
}
