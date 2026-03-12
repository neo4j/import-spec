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
package model.source

import kotlinx.serialization.Serializable
import model.Neo4jType
import kotlin.js.JsExport

@JsExport
@Serializable
data class TableField(
    val name: String,
    val type: String = "",
    val size: Int = 0, // TODO: Is size needed or used?
    val suggested: Neo4jType = Neo4jType.STRING,
    val supported: Set<Neo4jType> = emptySet()
)
