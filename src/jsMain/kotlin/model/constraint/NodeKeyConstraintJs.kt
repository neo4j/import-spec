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

import js.objects.ReadonlyRecord
import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodeKeyConstraintJs : NodeConstraintJs {
    val options: ReadonlyRecord<String, Any>
}

fun nodeKeyConstraintJs(type: String, label: String, properties: Array<String>, options: ReadonlyRecord<String, Any>) =
    object : NodeKeyConstraintJs {
        override val type = type
        override val label = label
        override val properties = properties
        override val options = options
    }
