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
import js.objects.Record
import js.objects.buildRecord
import js.objects.toRecord
import model.GraphModel
import model.GraphModelJs
import model.NodeJs
import model.associateBy
import model.graphModelJs
import model.labelsJs
import model.mapping.toClass
import model.mapping.toJs
import model.nodeJs
import model.source.toClass
import model.source.toJs
import model.toClass
import model.toJs
import model.toMap

/**
 * We have duplicate model built on external interfaces with conversion to and from classes in order
 * to support plain JavaScript objects which are used in React's Redux state storage.
 */
@JsExport
class GraphSpecEditor {
    companion object {
        @JsStatic
        fun plain(model: GraphModel): GraphModelJs = graphModelJs(
            version = model.version,
            nodes = model.nodes.mapValues { (_, node) -> node.toJs() }.toRecord(),
            relationships = model.relationships.mapValues { (_, relationship) -> relationship.toJs() }.toRecord(),
            tables = model.tables.mapValues { (_, table) -> table.toJs() }.toRecord(),
            mappings = model.mappings.map { mapping -> mapping.toJs() }.toTypedArray()
        )

        @JsStatic
        fun model(model: GraphModelJs): GraphModel = GraphModel(
            version = model.version,
            nodes = model.nodes.associateBy { id, js -> js.toClass(id) },
            relationships = model.relationships.associateBy { id, js -> js.toClass(id) },
            tables = model.tables.associateBy { _, js -> js.toClass() },
            mappings = model.mappings.map { it.toClass() }
        )

        @JsStatic
        fun addNode(nodes: Record<String, NodeJs>, id: String) = buildRecord {
            for ((key, value) in nodes.toMap()) {
                set(key, value) // TODO does it need to be a deep copy? - Yes
            }
            set(id, nodeJs())
        }

        @JsStatic
        fun addNodeInline(model: GraphModelJs, id: String) {
            model.nodes[id] = nodeJs()
        }

        @JsStatic
        fun addNodeLabelInline(model: GraphModelJs, id: String, label: String) {
            val node = model.nodes[id] ?: return // TODO how to handle missing nodes
            node.labels.implied += label
        }

        @JsStatic
        fun addNodeLabel(model: GraphModelJs, id: String, label: String): GraphModelJs {
            val copy = model.nodes.toMap().toMutableMap()
            copy[id] = addLabel(model.nodes[id] ?: return model, label)
            return graphModelJs(
                version = model.version,
                nodes = copy.toRecord(),
                relationships = model.relationships,
                tables = model.tables,
                mappings = model.mappings
            )
        }

        @JsStatic
        fun addLabel(node: NodeJs, label: String) = nodeJs(
            labels = labelsJs(
                identifier = node.labels.identifier,
                implied = node.labels.implied + label,
                optional = node.labels.optional
            ),
            properties = node.properties,
            constraints = node.constraints,
            indexes = node.indexes,
            extensions = node.extensions
        )
    }
}
