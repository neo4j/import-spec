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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.function.Function;

public interface SourceProvider<T extends Source> extends Function<ObjectNode, T> {

    /**
     * This declares the source type this source provider supports
     * Please implement {@link SourceProvider#supportsType(String)} instead.
     * @return the source type supported by this provider
     */
    @Deprecated(forRemoval = true)
    default String supportedType() {
        return "###unset";
    }

    /**
     * This provides a flexible to tell is the type is supported by this source provider
     * @return true if this source provider supports the type, false otherwise
     */
    default boolean supportsType(String type) {
        return supportedType().toLowerCase(Locale.ROOT).equals(type.toLowerCase(Locale.ROOT));
    }
}
