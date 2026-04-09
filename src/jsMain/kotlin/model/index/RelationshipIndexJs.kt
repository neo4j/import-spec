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
package model.index

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.associateBy
import model.emptyRecord
import model.extension.ExtensionValueJs
import model.extension.toClass
import model.extension.toJs
import model.jso
import model.toMap

@JsExport
@JsPlainObject
external interface RelationshipIndexJs {
    val type: String
    val properties: Array<String>
    val options: Record<String, ExtensionValueJs>
    val extensions: Record<String, ExtensionValueJs>
}

fun relationshipIndexJs(
    type: String,
    properties: Array<String>,
    options: Record<String, ExtensionValueJs> = emptyRecord(),
    extensions: Record<String, ExtensionValueJs> = emptyRecord()
): RelationshipIndexJs = jso {
    this.type = type
    this.properties = properties
    this.options = options
    this.extensions = extensions
}

fun RelationshipIndex.toJs() = relationshipIndexJs(
    type = type,
    properties = properties.toTypedArray(),
    options = options.associateBy { _, value -> value.toJs() },
    extensions = extensions.associateBy { _, value -> value.toJs() }
)

fun RelationshipIndexJs.toClass() = RelationshipIndex(
    type = type,
    properties = properties.toSet(),
    options = options.associateBy { _, value -> value.toClass() },
    extensions = extensions.associateBy { _, value -> value.toClass() }.toMutableMap()
)
