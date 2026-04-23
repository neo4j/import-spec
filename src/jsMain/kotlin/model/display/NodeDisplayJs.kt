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
package model.display

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.associateBy
import model.emptyRecord
import model.extension.ExtensionValueJs
import model.extension.toClass
import model.extension.toJs
import model.jso
import kotlin.collections.component1
import kotlin.collections.component2

@JsExport
@JsPlainObject
external interface NodeDisplayJs {
    var x: Double
    var y: Double
    val extensions: Record<String, ExtensionValueJs>
}

fun nodeDisplayJs(x: Double, y: Double, extensions: Record<String, ExtensionValueJs> = emptyRecord()): NodeDisplayJs =
    jso {
        this.x = x
        this.y = y
        this.extensions = extensions
    }

fun NodeDisplay.toJs() = nodeDisplayJs(
    x = x,
    y = y,
    extensions = extensions.mapValues { (_, extension) -> extension.toJs() }.toRecord()
)

fun NodeDisplayJs.toClass(): NodeDisplay = NodeDisplay(
    x = x,
    y = y,
    extensions = extensions.associateBy { _, value -> value.toClass() }.toMutableMap()
)
