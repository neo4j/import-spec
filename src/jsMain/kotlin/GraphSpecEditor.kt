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
import js.objects.emptyReadonlyRecord
import model.GraphModel
import model.GraphModelJs
import model.NodeJs
import model.RelationshipJs

@JsExport
class GraphSpecEditor {
    companion object {
        @JsStatic
        fun plain(model: GraphModel): GraphModelJs {
            val version = model.version
            val nodes = emptyReadonlyRecord<String, NodeJs>()
            val relationships = emptyReadonlyRecord<String, RelationshipJs>()
            return object : GraphModelJs {
                override val version = version
                override val nodes = nodes
                override val relationships = relationships
            }
        }

        @JsStatic
        fun model(model: GraphModelJs): GraphModel = GraphModel(model.version)

        fun addNode(nodes: Map<String, NodeJs>): Map<String, NodeJs> = nodes
    }
}
