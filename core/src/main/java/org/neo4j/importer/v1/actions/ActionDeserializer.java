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
package org.neo4j.importer.v1.actions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.validation.NotExactlyOneActionProviderException;
import org.neo4j.importer.v1.validation.UndeserializableActionIOException;

public class ActionDeserializer extends StdDeserializer<Action> {

    private final List<ActionProvider<? extends Action>> providers;

    public ActionDeserializer(List<ActionProvider<? extends Action>> providers) {
        super(Action.class);
        this.providers = providers;
    }

    @Override
    public Action deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
        ObjectNode node = p.getCodec().readTree(p);
        var type = node.get("type").textValue();
        var provider = findActionProviderByType(type);
        try {
            return provider.apply(node.deepCopy());
        } catch (Exception e) {
            throw new UndeserializableActionIOException(node, provider, e);
        }
    }

    private ActionProvider<? extends Action> findActionProviderByType(String type) {
        var providers = this.providers.stream()
                .filter(provider ->
                        provider.supportedType().toLowerCase(Locale.ROOT).equals(type.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        var count = providers.size();
        if (count != 1) {
            throw new NotExactlyOneActionProviderException(type, count);
        }
        return providers.iterator().next();
    }
}
