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
package model

import js.objects.Record
import kotlinx.js.JsPlainObject
import model.display.DisplayJs
import model.display.displayJs
import model.mapping.MappingJs
import model.node.NodeJs
import model.relationship.RelationshipJs
import model.source.TableJs

@JsExport
@JsPlainObject
external interface GraphModelJs {
    val version: String
    val nodes: Record<String, NodeJs>
    val relationships: Record<String, RelationshipJs>
    val tables: Record<String, TableJs>
    var mappings: Array<MappingJs>
    val display: DisplayJs
}

fun graphModelJs(
    version: String,
    nodes: Record<String, NodeJs> = emptyRecord(),
    relationships: Record<String, RelationshipJs> = emptyRecord(),
    tables: Record<String, TableJs> = emptyRecord(),
    mappings: Array<MappingJs> = emptyArray(),
    display: DisplayJs = displayJs()
): GraphModelJs = jso {
    this.version = version
    this.nodes = nodes
    this.relationships = relationships
    this.tables = tables
    this.mappings = mappings
    this.display = display
}
