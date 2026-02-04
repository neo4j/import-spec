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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionProvider;
import org.neo4j.importer.v1.actions.ActionStage;

public class CypherActionProvider implements ActionProvider<CypherAction> {

    @Override
    public String supportedType() {
        return "cypher";
    }

    @Override
    public CypherAction apply(ObjectNode node) {
        JsonNode active = node.get("active");
        return new CypherAction(
                active == null ? Action.DEFAULT_ACTIVE : Boolean.parseBoolean(active.textValue()),
                node.get("name").textValue(),
                ActionStage.valueOf(node.get("stage").textValue().toUpperCase(Locale.ROOT)),
                node.get("query").asText(""), // "" will trigger a downstream validation error
                parseExecutionModeLeniently(node));
    }

    private static CypherExecutionMode parseExecutionModeLeniently(ObjectNode node) {
        if (!node.has("execution_mode")) {
            return CypherAction.DEFAULT_CYPHER_EXECUTION_MODE;
        }
        try {
            return CypherExecutionMode.valueOf(
                    node.get("execution_mode").asText().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null; // null will trigger a downstream validation error
        }
    }
}
