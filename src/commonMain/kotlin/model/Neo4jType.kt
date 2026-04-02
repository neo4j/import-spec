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
package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.js.JsExport

@JsExport
@Serializable
enum class Neo4jType {
    ANY,
    BOOLEAN,

    @SerialName("LIST<BOOLEAN>")
    LIST_BOOLEAN,

    DATE,

    @SerialName("LIST<DATE>")
    LIST_DATE,
    DURATION,

    @SerialName("LIST<DURATION>")
    LIST_DURATION,
    FLOAT32,

    @SerialName("LIST<FLOAT32>")
    LIST_FLOAT32,

    @JsonNames("FLOAT")
    @SerialName("FLOAT")
    FLOAT64,

    @JsonNames("LIST<FLOAT>")
    @SerialName("LIST<FLOAT>")
    LIST_FLOAT64,
    INTEGER8,

    @SerialName("LIST<INTEGER8>")
    LIST_INTEGER8,
    INTEGER16,

    @SerialName("LIST<INTEGER16>")
    LIST_INTEGER16,
    INTEGER32,

    @SerialName("LIST<INTEGER32>")
    LIST_INTEGER32,

    @JsonNames("INTEGER")
    @SerialName("INTEGER")
    INTEGER64,

    @JsonNames("LIST<INTEGER>")
    @SerialName("LIST<INTEGER>")
    LIST_INTEGER64,

    @JsonNames("LOCAL_DATETIME")
    @SerialName("LOCAL DATETIME")
    LOCAL_DATETIME,

    @JsonNames("LIST<LOCAL_DATETIME>")
    @SerialName("LIST<LOCAL DATETIME>")
    LIST_LOCAL_DATETIME,

    @JsonNames("LOCAL_TIME")
    @SerialName("LOCAL TIME")
    LOCAL_TIME,

    @JsonNames("LIST<LOCAL_TIME>")
    @SerialName("LIST<LOCAL TIME>")
    LIST_LOCAL_TIME,
    POINT,

    @SerialName("LIST<POINT>")
    LIST_POINT,
    STRING,

    @SerialName("LIST<STRING>")
    LIST_STRING,

    @SerialName("VECTOR<FLOAT>")
    VECTOR_FLOAT,

    @SerialName("VECTOR<FLOAT32>")
    VECTOR_FLOAT32,

    @SerialName("VECTOR<INTEGER>")
    VECTOR_INTEGER,

    @SerialName("VECTOR<INTEGER32>")
    VECTOR_INTEGER32,

    @SerialName("VECTOR<INTEGER16>")
    VECTOR_INTEGER16,

    @SerialName("VECTOR<INTEGER8>")
    VECTOR_INTEGER8,

    @JsonNames("ZONED_DATETIME")
    @SerialName("ZONED DATETIME")
    ZONED_DATETIME,

    @JsonNames("LIST<ZONED_DATETIME>")
    @SerialName("LIST<ZONED DATETIME>")
    LIST_ZONED_DATETIME,

    @JsonNames("ZONED_TIME")
    @SerialName("ZONED TIME")
    ZONED_TIME,

    @JsonNames("LIST<ZONED_TIME>")
    @SerialName("LIST<ZONED TIME>")
    LIST_ZONED_TIME,
}
