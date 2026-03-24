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
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.associateBy
import model.emptyRecord
import model.jso

@JsExport
@JsPlainObject
external interface TableJs {
    val source: String
    val fields: Record<String, TableFieldJs>
    val primaryKeys: Array<String>
    val foreignKeys: Record<String, ForeignKeyJs>
}

fun tableJs(
    source: String,
    fields: Record<String, TableFieldJs> = emptyRecord(),
    primaryKeys: Array<String> = emptyArray(),
    foreignKeys: Record<String, ForeignKeyJs> = emptyRecord(),
): TableJs = jso {
    this.source = source
    this.fields = fields
    this.primaryKeys = primaryKeys
    this.foreignKeys = foreignKeys
}

fun Table.toJs() = tableJs(
    source = source,
    fields = fields.mapValues { (_, field) -> field.toJs() }.toRecord(),
    primaryKeys = primaryKeys.toTypedArray(),
    foreignKeys = foreignKeys.mapValues { (_, key) -> key.toJs() }.toRecord(),
)

fun TableJs.toClass() = Table(
    source = source,
    fields = fields.associateBy { _, field -> field.toClass() },
    primaryKeys = primaryKeys.toSet(),
    foreignKeys = foreignKeys.associateBy { _, fk -> fk.toClass() }
)
