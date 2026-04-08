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
external interface TargetMappingJs {
    val node: String
    val label: String
    val properties: Record<String, PropertyMappingJs>
}

fun targetMappingJs(node: String, label: String, properties: Record<String, PropertyMappingJs>): TargetMappingJs = jso {
    this.node = node
    this.label = label
    this.properties = properties
}

fun TargetMapping.toJs() = targetMappingJs(
    node = node,
    label = label,
    properties = properties.map { it.key to it.value.toJs() }.toMap().toRecord()
)

fun TargetMappingJs.toClass() = TargetMapping(
    node = node,
    label = label,
    properties = properties.associateBy { _, value -> value.toClass() }
)
