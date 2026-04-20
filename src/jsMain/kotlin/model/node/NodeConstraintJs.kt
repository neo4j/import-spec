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
package model.node

import js.objects.Record
import kotlinx.js.JsPlainObject
import model.associateBy
import model.emptyRecord
import model.extension.ExtensionValueJs
import model.extension.toClass
import model.extension.toJs
import model.jso

@JsExport
@JsPlainObject
external interface NodeConstraintJs {
    var type: String
    var label: String
    var properties: Array<String>
    val extensions: Record<String, ExtensionValueJs>
}

fun nodeConstraintJs(
    type: String,
    label: String,
    properties: Array<String> = emptyArray(),
    extensions: Record<String, ExtensionValueJs> = emptyRecord()
): NodeConstraintJs = jso {
    this.type = type
    this.label = label
    this.properties = properties
    this.extensions = extensions
}

fun NodeConstraint.toJs() = nodeConstraintJs(
    type = type,
    label = label,
    properties = properties.toTypedArray(),
    extensions = extensions.associateBy { _, value -> value.toJs() }
)

fun NodeConstraintJs.toClass() = NodeConstraint(
    type = type,
    label = label,
    properties = properties.toSet(),
    extensions = extensions.associateBy { _, value -> value.toClass() }.toMutableMap()
)
