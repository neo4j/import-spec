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

import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.jso
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface TableFieldJs {
    val type: String
    val size: Int
    val suggested: String?
    val supported: Array<String>
}

fun tableFieldJs(
    type: String,
    size: Int = -1,
    suggested: String? = null,
    supported: Array<String> = emptyArray(),
): TableFieldJs = jso {
    this.type = type
    this.size = size
    this.suggested = suggested
    this.supported = supported
}

fun TableField.toJs() = tableFieldJs(
    type = type,
    size = size,
    suggested = suggested?.name,
    supported = supported.map { it.name }.toTypedArray(),
)

fun TableFieldJs.toClass() = TableField(
    type = type,
    size = size,
    suggested = suggested?.let { Neo4jType.valueOf(it) },
    supported = supported.map { Neo4jType.valueOf(it) }.toSet()
)
