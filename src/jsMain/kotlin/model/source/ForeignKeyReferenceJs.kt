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

import js.objects.Record
import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.associateBy
import model.emptyRecord
import model.extension.ExtensionValueJs
import model.extension.toClass
import model.extension.toJs
import model.jso
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface ForeignKeyReferenceJs {
    val table: String
    val fields: Array<String>
    val extensions: Record<String, ExtensionValueJs>
}

fun foreignKeyReferenceJs(
    table: String,
    fields: Array<String>,
    extensions: Record<String, ExtensionValueJs> = emptyRecord()
): ForeignKeyReferenceJs = jso {
    this.table = table
    this.fields = fields
    this.extensions = extensions
}

fun ForeignKeyReference.toJs() = foreignKeyReferenceJs(
    table = table,
    fields = fields.toTypedArray(),
    extensions = extensions.associateBy { _, value -> value.toJs() }
)

fun ForeignKeyReferenceJs.toClass() = ForeignKeyReference(
    table = table,
    fields = fields.toSet(),
    extensions = extensions.associateBy { _, value -> value.toClass() }.toMutableMap()
)
