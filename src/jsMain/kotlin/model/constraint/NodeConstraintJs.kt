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
package model.constraint

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.toMap

@JsExport
@JsPlainObject
external interface NodeConstraintJs {
    val type: String
    val label: String
    val properties: Array<String>
    val options: Record<String, Any>
}

fun nodeConstraintJs(type: String, label: String, properties: Array<String>, options: Record<String, Any>) =
    object : NodeConstraintJs {
        override val type = type
        override val label = label
        override val properties = properties
        override val options = options
    }

fun NodeConstraint.toJs(): NodeConstraintJs = nodeConstraintJs(
    type = type,
    label = label,
    properties = properties.toTypedArray(),
    options = options.toRecord()
)

fun NodeConstraintJs.toClass(): NodeConstraint = NodeConstraint(
    type = type,
    label = label,
    properties = properties.toSet(),
    options = options.toMap()
)
