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
package model.mapping

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.associateBy
import model.jso

@JsExport
@JsPlainObject
external interface NodeMappingJs : MappingJs {
    override val type: String
    val node: String
    val table: String
    val properties: Record<String, PropertyMappingJs>
    val mode: String
    val matchLabel: String?
    val keys: Array<String>
}

fun nodeMappingJs(
    node: String,
    table: String,
    properties: Record<String, PropertyMappingJs>,
    mode: String,
    matchLabel: String?,
    keys: Array<String>
): NodeMappingJs = jso {
    this.type = MappingType.NODE
    this.node = node
    this.table = table
    this.properties = properties
    this.mode = mode
    this.matchLabel = matchLabel
    this.keys = keys
}

fun NodeMapping.toJs() = nodeMappingJs(
    node = node,
    table = table,
    properties = properties.map { it.key to it.value.toJs() }.toMap().toRecord(),
    mode = mode.name,
    matchLabel = matchLabel,
    keys = keys.toTypedArray()
)

fun NodeMappingJs.toClass() = NodeMapping(
    node = node,
    table = table,
    properties = properties.associateBy { _, value -> value.toClass() },
    mode = MappingMode.valueOf(mode),
    matchLabel = matchLabel,
    keys = keys.toSet()
)
