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
external interface NodeIndexJs {
    var type: String
    var labels: Array<String>
    var properties: Array<String>
    val options: Record<String, ExtensionValueJs>
    val extensions: Record<String, ExtensionValueJs>
}

fun nodeIndexJs(
    type: String,
    labels: Array<String> = emptyArray(),
    properties: Array<String> = emptyArray(),
    options: Record<String, ExtensionValueJs> = emptyRecord(),
    extensions: Record<String, ExtensionValueJs> = emptyRecord()
): NodeIndexJs = jso {
    this.type = type
    this.labels = labels
    this.properties = properties
    this.options = options
    this.extensions = extensions
}

fun NodeIndex.toJs() = nodeIndexJs(
    type = type,
    labels = labels.toTypedArray(),
    properties = properties.toTypedArray(),
    options = options.associateBy { _, value -> value.toJs() },
    extensions = extensions.associateBy { _, value -> value.toJs() }
)

fun NodeIndexJs.toClass() = NodeIndex(
    type = type,
    labels = labels.toSet(),
    properties = properties.toSet(),
    options = options.associateBy { _, value -> value.toClass() },
    extensions = extensions.associateBy { _, value -> value.toClass() }.toMutableMap()
)
