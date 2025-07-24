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
package org.neo4j.importer.v1;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility class for serializing enums to JSON-like string formats to keep tests DRY.
 * The class is a singleton and cannot be instantiated.
 */
public final class ImportSpecificationDeserializerEnumUtil {

    private ImportSpecificationDeserializerEnumUtil() {
        /* singleton */
    }

    public static <E extends Enum<E>> String enumToJsonString(Class<E> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map((E e) -> "\"" + e.name().toLowerCase() + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
