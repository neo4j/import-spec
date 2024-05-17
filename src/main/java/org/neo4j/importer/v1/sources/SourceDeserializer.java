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
package org.neo4j.importer.v1.sources;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.validation.NotExactlyOneSourceProviderException;
import org.neo4j.importer.v1.validation.UndeserializableSourceIOException;

public class SourceDeserializer extends StdDeserializer<Source> {

    private final List<SourceProvider<? extends Source>> providers;

    public SourceDeserializer(List<SourceProvider<? extends Source>> providers) {
        super(Source.class);
        this.providers = providers;
    }

    @Override
    public Source deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectNode node = p.getCodec().readTree(p);
        var type = node.get("type").textValue();
        var provider = findSourceProviderByType(type);
        try {
            return provider.provide(node.deepCopy());
        } catch (Exception e) {
            throw new UndeserializableSourceIOException(node, provider, e);
        }
    }

    private SourceProvider<? extends Source> findSourceProviderByType(String type) {
        var providers = this.providers.stream()
                .filter(provider ->
                        provider.supportedType().toLowerCase(Locale.ROOT).equals(type.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        var count = providers.size();
        if (count != 1) {
            throw new NotExactlyOneSourceProviderException(type, count);
        }
        return providers.iterator().next();
    }
}
