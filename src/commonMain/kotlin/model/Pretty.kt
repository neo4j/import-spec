package model

import model.mapping.NodeMapping
import model.mapping.RelationshipMapping
import model.type.Named
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Removes any stable ids in favour of human-readable [Named.name]'s.
 */
object Pretty {
    fun prettify(model: GraphModel) {
        model.prettifyNodeLabels()
        model.prettifyNodes()
        model.prettifyNodeProperties()
        model.prettifyRelationships()
        model.prettifyRelationshipProperties()
    }

    /*
        Nodes
     */

    private fun GraphModel.prettifyNodeLabels() {
        nodes.values.forEach { node ->
            if (node.labels.implied.isEmpty() && node.labels.optional.isEmpty()) {
                node.label = node.labels.identifier
                node.labels.identifier = ""
            }
        }
    }

    private fun GraphModel.prettifyNodes() {
        val renames = nodes.prettify()
        renameNodeMappings(this, renames)
        nodes.values.forEach { node ->
            node.constraints.prettify()
            node.indexes.prettify()
        }
    }

    internal fun renameNodeMappings(model: GraphModel, renames: Map<String, String>) {
        model.mappings.filterIsInstance<NodeMapping>().forEach { mapping ->
            mapping.node = renames[mapping.node] ?: mapping.node
        }
        model.mappings.filterIsInstance<RelationshipMapping>().forEach { mapping ->
            mapping.from.node = renames[mapping.from.node] ?: mapping.from.node
            mapping.to.node = renames[mapping.to.node] ?: mapping.to.node
        }
    }

    private fun GraphModel.prettifyNodeProperties() {
        val renames = mutableMapOf<String, String>()
        nodes.forEach { (key, node) ->
            renames.putAll(node.properties.prettify(key))
        }
        renameNodeMappingProperties(this, renames)
    }

    internal fun renameNodeMappingProperties(model: GraphModel, renames: Map<String, String>) {
        model.mappings.filterIsInstance<NodeMapping>().forEach { mapping ->
            mapping.properties.rename(renames, mapping.node)
        }
        model.mappings.filterIsInstance<RelationshipMapping>().forEach { mapping ->
            mapping.from.properties.rename(renames, mapping.from.node)
            mapping.to.properties.rename(renames, mapping.to.node)
        }
    }

    /*
        Relationships
     */

    private fun GraphModel.prettifyRelationships() {
        val renames = relationships.prettify()
        renameRelationshipMappings(this, renames)
        relationships.values.forEach { node ->
            node.constraints.prettify()
            node.indexes.prettify()
        }
    }

    internal fun renameRelationshipMappings(model: GraphModel, renames: Map<String, String>) {
        model.mappings.filterIsInstance<RelationshipMapping>().forEach { mapping ->
            println("Set ${mapping.relationship} to ${renames[mapping.relationship] ?: mapping.relationship}")
            mapping.relationship = renames[mapping.relationship] ?: mapping.relationship
        }
    }

    private fun GraphModel.prettifyRelationshipProperties() {
        val renames = mutableMapOf<String, String>()
        relationships.forEach { (key, relationship) ->
            renames.putAll(relationship.properties.prettify(key))
        }
        renameRelationshipMappingProperties(this, renames)
    }

    internal fun renameRelationshipMappingProperties(model: GraphModel, renames: Map<String, String>) {
        model.mappings.filterIsInstance<RelationshipMapping>().forEach { mapping ->
            mapping.properties.rename(renames, mapping.relationship)
        }
    }

    /**
     * Goes through a MutableMap, replacing the keys with replacements from [renames]
     * @param parent optionally used to look up the replacement key
     */
    private fun <T> MutableMap<String, T>.rename(renames: Map<String, String>, parent: String? = null) {
        val original = toMutableMap()
        clear()
        for ((og, value) in original) {
            val key = if (parent != null) {
                renames["${parent}:${og}"]
            } else {
                renames[og]
            } ?: og
            this[key] = value
        }
    }

    /**
     * Removes any [Named.name]'s and places them as the key in the MutableMap
     * @param parent name to use as a key prefix in the @return map
     * @return Map of original keys to their replacements
     */
    private fun <T : Named> MutableMap<String, T>.prettify(parent: String? = null): Map<String, String> {
        val original = toMutableMap()
        clear()
        val changes = mutableMapOf<String, String>()
        for ((og, node) in original) {
            val key = node.name ?: og
            node.name = null
            this[key] = node
            if (parent != null) {
                changes["$parent:${og}"] = key
            } else {
                changes[og] = key
            }

        }
        return changes
    }
}
