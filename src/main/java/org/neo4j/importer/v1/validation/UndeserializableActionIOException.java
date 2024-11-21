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
package org.neo4j.importer.v1.validation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionProvider;

public class UndeserializableActionIOException extends IOException implements ActionError {

    public UndeserializableActionIOException(
            ObjectNode node, ActionProvider<? extends Action> provider, Throwable cause) {
        super(
                String.format(
                        "Action provider %s failed to deserialize the following action definition: %s",
                        provider.getClass().getName(), node.toString()),
                cause);
    }
}
