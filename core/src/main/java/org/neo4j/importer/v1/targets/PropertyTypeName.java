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
package org.neo4j.importer.v1.targets;

public enum PropertyTypeName {
    BOOLEAN,
    BOOLEAN_ARRAY,
    BYTE_ARRAY,
    DATE,
    DATE_ARRAY,
    DURATION,
    DURATION_ARRAY,
    FLOAT,
    FLOAT_ARRAY,
    INTEGER,
    INTEGER_ARRAY,
    LOCAL_DATETIME,
    LOCAL_DATETIME_ARRAY,
    LOCAL_TIME,
    LOCAL_TIME_ARRAY,
    POINT,
    POINT_ARRAY,
    STRING,
    STRING_ARRAY,
    ZONED_DATETIME,
    ZONED_DATETIME_ARRAY,
    ZONED_TIME,
    ZONED_TIME_ARRAY,
    INTEGER_VECTOR(true),
    FLOAT_VECTOR(true),
    FLOAT32_VECTOR(true),
    INTEGER8_VECTOR(true),
    INTEGER16_VECTOR(true),
    INTEGER32_VECTOR(true);

    private final boolean isVector;

    PropertyTypeName() {
        this(false);
    }

    PropertyTypeName(boolean isVector) {
        this.isVector = isVector;
    }

    public boolean isVector() {
        return isVector;
    }
}
