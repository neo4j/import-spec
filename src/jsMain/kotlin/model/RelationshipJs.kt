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

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.constraint.RelationshipConstraintJs
import model.constraint.toClass
import model.constraint.toJs
import model.extension.ExtensionValueJs
import model.extension.toClass
import model.extension.toJs
import model.index.RelationshipIndexJs
import model.index.toClass
import model.index.toJs

@JsExport
@JsPlainObject
external interface RelationshipJs {
    var type: String
    val from: RelationshipTargetJs
    val to: RelationshipTargetJs
    val properties: Record<String, PropertyJs>
    val constraints: Record<String, RelationshipConstraintJs>
    val indexes: Record<String, RelationshipIndexJs>
    val extensions: Record<String, ExtensionValueJs>
    var name: String
    val id: String
}

fun relationshipJs(
    type: String,
    from: RelationshipTargetJs,
    to: RelationshipTargetJs,
    properties: Record<String, PropertyJs> = emptyRecord(),
    constraints: Record<String, RelationshipConstraintJs> = emptyRecord(),
    indexes: Record<String, RelationshipIndexJs> = emptyRecord(),
    extensions: Record<String, ExtensionValueJs> = emptyRecord(),
    name: String,
    id: String
): RelationshipJs = jso {
    this.type = type
    this.from = from
    this.to = to
    this.properties = properties
    this.constraints = constraints
    this.indexes = indexes
    this.extensions = extensions
    this.name = name
    this.id = id
}

fun Relationship.toJs(id: String) = relationshipJs(
    type = type,
    from = from.toJs(),
    to = to.toJs(),
    properties = properties.mapValues { (key, property) -> property.toJs(key) }.toRecord(),
    constraints = constraints.mapValues { (_, constraint) -> constraint.toJs() }.toRecord(),
    indexes = indexes.mapValues { (_, index) -> index.toJs() }.toRecord(),
    extensions = extensions.mapValues { (_, extension) -> extension.toJs() }.toRecord(),
    name = name ?: id,
    id = id
)

fun RelationshipJs.toClass(id: String) = Relationship(
    type = type,
    from = from.toClass(),
    to = to.toClass(),
    properties = properties.toMap().mapValues { (name, property) -> property.toClass("relationships.$id", name) },
    constraints = constraints.toMap().mapValues { (_, constraint) -> constraint.toClass() },
    indexes = indexes.toMap().mapValues { (_, index) -> index.toClass() },
    extensions = extensions.toMap().mapValues { (_, value) -> value.toClass() }.toMutableMap(),
    name = name
)
