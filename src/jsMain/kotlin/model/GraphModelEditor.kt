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

import js.objects.toRecord
import model.display.toClass
import model.display.toJs
import model.mapping.toClass
import model.mapping.toJs
import model.node.nodeJs
import model.node.toClass
import model.node.toJs
import model.relationship.relationshipJs
import model.relationship.toClass
import model.relationship.toJs
import model.source.toClass
import model.source.toJs

/**
 * We have duplicate model built on external interfaces with conversion to and from classes in order
 * to support plain JavaScript objects which are used in React's Redux state storage.
 */
@JsExport
class GraphModelEditor {
    companion object {
        @JsStatic
        fun plain(model: GraphModel): GraphModelJs = graphModelJs(
            version = model.version,
            nodes = model.nodes.mapValues { (key, node) -> node.toJs(key) }.toRecord(),
            relationships = model.relationships.mapValues { (id, relationship) -> relationship.toJs(id) }.toRecord(),
            tables = model.tables.mapValues { (_, table) -> table.toJs() }.toRecord(),
            mappings = model.mappings.map { mapping -> mapping.toJs() }.toTypedArray(),
            display = model.display.toJs()
        )

        @JsStatic
        fun model(model: GraphModelJs): GraphModel = GraphModel(
            version = model.version,
            nodes = model.nodes.associateBy { id, js -> js.toClass(id) },
            relationships = model.relationships.associateBy { id, js -> js.toClass(id) },
            tables = model.tables.associateBy { _, js -> js.toClass() },
            mappings = model.mappings.map { it.toClass() },
            display = model.display.toClass()
        )

        @JsStatic
        fun addNode(model: GraphModelJs, name: String?): String = model.nodes.addUnique("node") { nodeId ->
            nodeJs(id = nodeId, name = name ?: nodeId)
        }

        @JsStatic
        fun removeNode(model: GraphModelJs, nodeId: String) {
            model.nodes.remove(nodeId)
        }

        @JsStatic
        fun addRelationship(model: GraphModelJs, type: String, name: String?): String =
            model.relationships.addUnique("relationship") { relId ->
                relationshipJs(type = type, id = relId, name = name ?: relId)
            }

        @JsStatic
        fun removeRelationship(model: GraphModelJs, relationshipId: String) {
            model.relationships.remove(relationshipId)
        }
    }
}
