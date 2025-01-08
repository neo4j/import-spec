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
package org.neo4j.importer.v1.actions.plugin;

import java.util.Objects;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionStage;

public class CypherAction implements Action {
    static final CypherExecutionMode DEFAULT_CYPHER_EXECUTION_MODE = CypherExecutionMode.TRANSACTION;

    private final boolean active;
    private final String name;
    private final ActionStage stage;
    private final String query;
    private final CypherExecutionMode executionMode;

    public CypherAction(
            boolean active, String name, ActionStage stage, String query, CypherExecutionMode executionMode) {
        this.active = active;
        this.name = name;
        this.stage = stage;
        this.query = query;
        this.executionMode = executionMode;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getType() {
        return "cypher";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ActionStage getStage() {
        return stage;
    }

    public String getQuery() {
        return query;
    }

    public CypherExecutionMode getExecutionMode() {
        return executionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CypherAction)) return false;
        CypherAction that = (CypherAction) o;
        return active == that.active
                && Objects.equals(name, that.name)
                && stage == that.stage
                && Objects.equals(query, that.query)
                && executionMode == that.executionMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, name, stage, query, executionMode);
    }

    @Override
    public String toString() {
        return "CypherAction{" + "active="
                + active + ", name='"
                + name + '\'' + ", stage="
                + stage + ", query='"
                + query + '\'' + ", executionMode="
                + executionMode + '}';
    }
}
