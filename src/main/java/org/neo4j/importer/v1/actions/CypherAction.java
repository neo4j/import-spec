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
package org.neo4j.importer.v1.actions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class CypherAction extends Action {
    private static final String DEFAULT_CYPHER_EXECUTION_MODE = "TRANSACTION";

    private final String query;
    private final CypherExecutionMode executionMode;

    @JsonCreator
    public CypherAction(
            @JsonProperty(value = "active", defaultValue = DEFAULT_ACTIVE) Boolean active,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty("stage") ActionStage stage,
            @JsonProperty(value = "query", required = true) String query,
            @JsonProperty(value = "execution_mode", defaultValue = DEFAULT_CYPHER_EXECUTION_MODE)
                    CypherExecutionMode executionMode) {

        super(active, name, ActionType.CYPHER, stage);
        this.query = query;
        this.executionMode =
                executionMode != null ? executionMode : CypherExecutionMode.valueOf(DEFAULT_CYPHER_EXECUTION_MODE);
    }

    public String getQuery() {
        return query;
    }

    public CypherExecutionMode getExecutionMode() {
        return executionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CypherAction that = (CypherAction) o;
        return Objects.equals(query, that.query) && executionMode == that.executionMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), query, executionMode);
    }

    @Override
    public String toString() {
        return "CypherAction{" + "query='"
                + query + '\'' + ", executionMode="
                + executionMode + "} "
                + super.toString();
    }
}
