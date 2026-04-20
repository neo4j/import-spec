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
external interface LabelsJs {
    var identifier: String
    var implied: Array<String>
    var optional: Array<String>
    val extensions: Record<String, ExtensionValueJs>
}

fun labelsJs(
    identifier: String = "",
    implied: Array<String> = emptyArray(),
    optional: Array<String> = emptyArray(),
    extensions: Record<String, ExtensionValueJs> = emptyRecord()
): LabelsJs = jso {
    this.identifier = identifier
    this.implied = implied
    this.optional = optional
    this.extensions = extensions
}

fun Labels.toJs() = labelsJs(
    identifier = identifier,
    implied = implied.toTypedArray(),
    optional = optional.toTypedArray(),
    extensions = extensions.associateBy { _, value -> value.toJs() }
)

fun LabelsJs.toClass(): Labels = Labels(
    identifier = identifier,
    implied = implied.toSet(),
    optional = optional.toSet(),
    extensions = extensions.associateBy { _, value -> value.toClass() }.toMutableMap()
)
