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
package model.source

import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.jso
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface ForeignKeyJs {
    val fields: Array<String>
    val references: ForeignKeyReferenceJs
}

fun foreignKeyJs(fields: Array<String>, references: ForeignKeyReferenceJs): ForeignKeyJs = jso {
    this.fields = fields
    this.references = references
}

fun ForeignKey.toJs() = foreignKeyJs(
    fields = fields.toTypedArray(),
    references = references.toJs()
)

fun ForeignKeyJs.toClass() = ForeignKey(
    fields = fields.toSet(),
    references = references.toClass()
)
