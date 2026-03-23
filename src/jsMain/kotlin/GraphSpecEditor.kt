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
import model.RelationshipJs
import model.graphModelJs
import model.mapping.MappingJs
import model.nodeJs
import model.source.TableJs
import model.toMap

@JsExport
class GraphSpecEditor {
    companion object {
        @JsStatic
        fun plain(model: GraphModel): GraphModelJs {
            val version = model.version
            val nodes = emptyMap<String, NodeJs>().toRecord()
            val relationships = emptyMap<String, RelationshipJs>().toRecord()
            val tables = emptyMap<String, TableJs>().toRecord()
            val mappings = emptyArray<MappingJs>()
            return object : GraphModelJs {
                override val version = version
                override val nodes = nodes
                override val relationships = relationships
                override val tables = tables
                override val mappings = mappings
            }
        }

        @JsStatic
        fun model(model: GraphModelJs): GraphModel = GraphModel(model.version)

        @JsStatic
        fun addNode(nodes: Record<String, NodeJs>, id: String) = buildRecord {
            for ((key, value) in nodes.toMap()) {
                set(key, value) // TODO does it need to be a deep copy? - Yes
            }
            set(id, nodeJs())
        }

        // Testing - don't know how this will look
        @JsStatic
        fun GraphModelJs.addNodeLabel(id: String, label: String): GraphModelJs {
            val copy = nodes.toMap().toMutableMap()
            copy[id] = addLabel(nodes[id] ?: return this, label)
            return graphModelJs(
                version = version,
                nodes = copy.toRecord(),
                relationships = relationships,
                tables = tables,
                mappings = mappings
            )
        }

        @JsStatic
        fun addLabel(node: NodeJs, label: String) = nodeJs(
            labels = node.labels + label,
            properties = node.properties,
            constraints = node.constraints,
            indexes = node.indexes,
            extensions = node.extensions
        )
    }
}
