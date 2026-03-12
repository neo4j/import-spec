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

import js.objects.ReadonlyRecord
import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.toMap

@JsExport
@JsPlainObject
external interface NodeIndexJs : IndexJs {
    val labels: Array<String>
    val properties: Array<String>
    val options: ReadonlyRecord<String, Any>
}

fun nodeIndexJs(kind: String, labels: Array<String>, properties: Array<String>, options: ReadonlyRecord<String, Any>) =
    object : NodeIndexJs {
        override val kind = kind
        override val labels = labels
        override val properties = properties
        override val options = options
    }

fun NodeIndex.toJs(): NodeIndexJs = nodeIndexJs(
    kind = kind,
    labels = labels.toTypedArray(),
    properties = properties.toTypedArray(),
    options = options.toReadonlyRecord()
)

fun NodeIndexJs.toClass(): NodeIndex = NodeIndex(
    kind = kind,
    labels = labels.toSet(),
    properties = properties.toSet(),
    options = options.toMap()
)
