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

import model.type.Named

/**
 * Gives all ids in the GraphModel a stable, sortable and predictable id.
 * Moving all human-readable ids into [Named.name]
 */
object Internal {
    fun internalise(model: GraphModel) {
        model.internaliseNodeLabels()
        model.internaliseNodes()
        model.internaliseNodeProperties()
        model.internaliseRelationships()
        model.internaliseRelationshipProperties()
    }

    /*
        Nodes
     */

    private fun GraphModel.internaliseNodeLabels() {
        nodes.values.forEach { node ->
            val label = node.label
            if (label != null && node.labels.identifier.isBlank()) {
                node.labels.identifier = label
                node.label = null
            }
        }
    }

    private fun GraphModel.internaliseNodes() {
        val renames = nodes.identify("node")
        Pretty.renameNodeMappings(this, renames)
        nodes.values.forEach { node ->
            node.constraints.identify("nodeConstraint")
            node.indexes.identify("nodeIndex")
        }
    }

    private fun GraphModel.internaliseNodeProperties() {
        val renames = mutableMapOf<String, String>()
        nodes.forEach { (key, node) ->
            renames.putAll(node.properties.identify("nodeProperty", key))
        }
        Pretty.renameNodeMappingProperties(this, renames)
    }

    /*
        Relationships
     */

    private fun GraphModel.internaliseRelationships() {
        val renames = relationships.identify("relationship")
        Pretty.renameRelationshipMappings(this, renames)
        relationships.values.forEach { node ->
            node.constraints.identify("relationshipConstraint")
            node.indexes.identify("relationshipIndex")
        }
    }

    private fun GraphModel.internaliseRelationshipProperties() {
        val renames = mutableMapOf<String, String>()
        relationships.forEach { (key, relationship) ->
            renames.putAll(relationship.properties.identify("relationshipProperty", key))
        }
        Pretty.renameRelationshipMappingProperties(this, renames)
    }

    /**
     * Replaces every key in the MutableMap with a predictable stable id.
     * Pushing existing keys into [Named.name]
     *
     * @param type The type of field in use to prefix the stable id e.g: node0, node1, node2 etc...
     * @param parent The parent field type to avoid stable id conflicts in a global map node0:property1, node0:property1
     * @return Map of original keys to their replacements
     */
    private fun <T : Named> MutableMap<String, T>.identify(type: String, parent: String? = null): Map<String, String> {
        val original = toMutableMap()
        clear()
        var i = 0
        val changes = mutableMapOf<String, String>()
        for ((name, node) in original) {
            node.name = name
            val key = "${type}${i++}"
            this[key] = node
            val changeKey = if (parent != null) {
                "$parent:$name"
            } else {
                name
            }
            changes[changeKey] = key
        }
        return changes
    }
}
