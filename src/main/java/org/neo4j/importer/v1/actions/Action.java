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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@Type(value = HttpAction.class, name = "http"), @Type(value = CypherAction.class, name = "cypher")})
public abstract class Action {

    protected static final String DEFAULT_ACTIVE = "true";
    private final boolean active;
    private final String name;
    private final ActionType type;
    private final ActionStage stage;

    protected Action(Boolean active, String name, ActionType type, ActionStage stage) {
        this.active = active != null ? active : Boolean.valueOf(DEFAULT_ACTIVE).booleanValue();
        this.name = name;
        this.type = type;
        this.stage = stage;
    }

    public boolean isActive() {
        return active;
    }

    public String getName() {
        return name;
    }

    public ActionType getType() {
        return type;
    }

    public ActionStage getStage() {
        return stage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return active == action.active
                && Objects.equals(name, action.name)
                && type == action.type
                && Objects.equals(stage, action.stage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, name, type, stage);
    }

    @Override
    public String toString() {
        return "Action{" + "active="
                + active + ", name='"
                + name + '\'' + ", type="
                + type + ", stage='"
                + stage + '\'' + '}';
    }
}
