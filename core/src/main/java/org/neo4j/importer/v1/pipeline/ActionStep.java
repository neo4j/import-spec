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
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionStage;

public class ActionStep implements ImportStep {

    private final Action action;
    private final Set<ImportStep> dependencies;

    ActionStep(Action action, Set<ImportStep> dependencies) {
        this.action = action;
        this.dependencies = dependencies;
    }

    @Override
    public String name() {
        return action.getName();
    }

    public ActionStage stage() {
        return action().getStage();
    }

    public Action action() {
        return action;
    }

    @Override
    public Set<ImportStep> dependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActionStep)) return false;
        ActionStep that = (ActionStep) o;
        return Objects.equals(action, that.action) && Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, dependencies);
    }

    @Override
    public String toString() {
        return "ActionStep{" + "action=" + action + ", dependencies=" + dependencies + '}';
    }
}
