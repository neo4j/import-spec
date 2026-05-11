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
external interface RelationshipMappingJs : MappingJs {
    override val type: String
    var relationship: String
    var table: String
    val from: TargetMappingJs
    val to: TargetMappingJs
    val properties: Record<String, PropertyMappingJs>
    var mode: String
    var matchLabel: String?
    var keys: Array<String>
}

fun relationshipMappingJs(
    relationship: String,
    table: String,
    from: TargetMappingJs,
    to: TargetMappingJs,
    properties: Record<String, PropertyMappingJs>,
    mode: String,
    matchLabel: String?,
    keys: Array<String>
): RelationshipMappingJs = jso {
    this.type = MappingType.RELATIONSHIP
    this.relationship = relationship
    this.table = table
    this.from = from
    this.to = to
    this.properties = properties
    this.mode = mode
    this.matchLabel = matchLabel
    this.keys = keys
}

fun RelationshipMapping.toJs() = relationshipMappingJs(
    relationship = relationship,
    table = table,
    from = from.toJs(),
    to = to.toJs(),
    properties = properties.map { it.key to it.value.toJs() }.toMap().toRecord(),
    mode = mode.name,
    matchLabel = matchLabel,
    keys = keys.toTypedArray()
)

fun RelationshipMappingJs.toClass() = RelationshipMapping(
    relationship = relationship,
    table = table,
    from = from.toClass(),
    to = to.toClass(),
    properties = properties.associateBy { _, value -> value.toClass() },
    mode = MappingMode.valueOf(mode),
    matchLabel = matchLabel,
    keys = keys.toMutableSet()
)
