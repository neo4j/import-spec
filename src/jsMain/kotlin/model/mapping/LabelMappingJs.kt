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

import kotlinx.js.JsPlainObject
import model.jso

@JsExport
@JsPlainObject
external interface LabelMappingJs : MappingJs {
    override val type: String
    var table: String
    var field: String
}

fun labelMappingJs(table: String, field: String): LabelMappingJs = jso {
    this.type = MappingType.LABEL
    this.table = table
    this.field = field
}

fun LabelMapping.toJs() = labelMappingJs(
    table = table,
    field = field
)

fun LabelMappingJs.toClass() = LabelMapping(
    table = table,
    field = field
)
