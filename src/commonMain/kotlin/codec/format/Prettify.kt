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
package codec.format

import codec.schema.SchemaElement
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import model.constraint.ConstraintType
import net.pearx.kasechange.toCamelCase
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

object Prettify {
    fun transform(schema: SchemaMap): SchemaMap {
        rename(schema)
        tidyConstraints(schema)
        sort(schema)
        return schema
    }

    private fun rename(schema: SchemaMap) {
        val nodePropertyIds = mutableMapOf<String, String>()
        val (nodes, nodeIds) = prettify(
            schema,
            "nodes",
            name = {
                (
                    it.stringOrNull("name")
                        ?: (it.listOrNull("labels")?.firstOrNull() as? SchemaLiteral)?.string
                    )?.toCamelCase()
            }
        ) { nodeId, node ->
            prettifyProperties(node, nodePropertyIds, nodeId)
            updatePropertiesList(node, nodePropertyIds, nodeId, "constraints")
            updatePropertiesList(node, nodePropertyIds, nodeId, "indexes")
        }
        schema["nodes"] = nodes
        val relationshipPropertyIds = mutableMapOf<String, String>()
        val (relationships, relationshipIds) = prettify(
            schema,
            "relationships",
            name = { it.stringOrNull("type")?.toCamelCase() } // TODO fallback to combo of from and to names
        ) { _, relationship ->
            val type = relationship.string("type")
            val from = relationship.string("from")
            relationship["from"] = nodeIds.getOrElse(from) { from }
            val to = relationship.string("to")
            relationship["to"] = nodeIds.getOrElse(to) { to }
            prettifyProperties(relationship, relationshipPropertyIds, type)
            updatePropertiesList(relationship, relationshipPropertyIds, type, "constraints")
            updatePropertiesList(relationship, relationshipPropertyIds, type, "indexes")
        }
        schema["relationships"] = relationships
        val mappings = schema.listOfMapsOrNull("mappings")
        if (mappings != null) {
            val newMappings = mutableListOf<SchemaElement>()
            for (mapping in mappings) {
                val node = mapping.stringOrNull("node")
                val type = mapping.stringOrNull("type")
                if (node != null) {
                    mapping["node"] = nodeIds.getOrElse(node) { node }
                    updateProperties(mapping, nodePropertyIds, node)
                } else if (type != null) {
                    mapping["type"] = relationshipIds.getOrElse(type) { type }
                    updateProperties(mapping, relationshipPropertyIds, type)
                    val from = mapping.map("from")
                    val fromNode = from.string("node")
                    from["node"] = nodeIds.getOrElse(fromNode) { fromNode }
                    updateProperties(from, nodePropertyIds, fromNode)
                    val to = mapping.map("to")
                    val toNode = to.string("node")
                    to["node"] = nodeIds.getOrElse(toNode) { toNode }
                    updateProperties(to, nodePropertyIds, toNode)
                }
                newMappings.add(mapping)
            }
            schema["mappings"] = newMappings
        }
        if (schema.listOrNull("mappings")?.isEmpty() == true) {
            schema.remove("mappings")
        }
        if (schema.mapOrNull("tables")?.isEmpty() == true) {
            schema.remove("tables")
        }
    }

    private fun updatePropertiesList(
        node: SchemaMap,
        nodePropertyIds: MutableMap<String, String>,
        nodeId: String,
        key: String
    ) {
        val elements = node.mapOfMapsOrNull(key) ?: return
        for (constraint in elements.values) {
            updatePropertiesList(constraint, nodePropertyIds, nodeId)
        }
    }

    fun sort(schema: SchemaMap) {
        var preferredOrder = listOf("labels", "properties", "constraints", "indexes", "extensions")
        val nodes = schema.mapOfMaps("nodes")
        for (node in nodes.values) {
            for (key in preferredOrder) {
                val temp = node.remove(key) ?: continue
                node[key] = temp
            }
        }
        preferredOrder = listOf("type", "from", "to", "properties", "constraints", "indexes", "extensions")
        val relationships = schema.mapOfMaps("relationships")
        for (relationship in relationships.values) {
            for (key in preferredOrder) {
                val temp = relationship.remove(key) ?: continue
                relationship[key] = temp
            }
        }
    }

    fun tidyConstraints(schema: SchemaMap) {
        for ((_, node) in schema.mapOfMaps("nodes")) {
            val constraints = node.mapOrNull("constraints") ?: continue
            val nodeProperties = node.mapOfMaps("properties")
            val toRemove = mutableSetOf<String>()
            for ((key, constraint) in constraints) {
                constraint as? SchemaMap ?: error("Expected map at ${constraint.path}.$key")
                val properties = constraint.list("properties")
                if (properties.size == 1) {
                    val propertyId = (properties.single() as? SchemaLiteral)?.string
                        ?: error("Expected property string at ${properties.path}[0]")
                    val property = nodeProperties[propertyId] ?: error("Unable to find property $propertyId")
                    val type = constraint.string("type")
                    when (type) {
                        ConstraintType.EXISTS.name -> {
                            property["nullable"] = false
                            toRemove.add(key)
                        }
                        ConstraintType.UNIQUE.name -> {
                            property["unique"] = true
                            toRemove.add(key)
                        }
                        ConstraintType.KEY.name -> {
                            property["nullable"] = false
                            property["unique"] = true
                            toRemove.add(key)
                        }
                    }
                }
            }
            for (key in toRemove) {
                constraints.remove(key)
            }
            if (constraints.isEmpty()) {
                node.remove("constraints")
            }
        }
    }

    private fun prettifyProperties(node: SchemaMap, nodePropertyIds: MutableMap<String, String>, id: String) {
        val (nodeProperties, ids) = prettify(node, "properties")
        nodePropertyIds.putAll(ids.map { "$id|${it.key}" to it.value })
        if (nodeProperties.isNotEmpty()) {
            node["properties"] = nodeProperties
        } else {
            node.remove("properties")
        }
    }

    private fun updateProperties(mapping: SchemaMap, propertyIds: MutableMap<String, String>, parent: String) {
        val properties = mutableMapOf<String, SchemaElement>()
        for ((id, property) in mapping.mapOrNull("properties") ?: return) {
            val propertyId = propertyIds.getOrElse("$parent|$id") { id }
            properties[propertyId] = property
        }
        mapping["properties"] = properties
    }

    private fun updatePropertiesList(mapping: SchemaMap, propertyIds: MutableMap<String, String>, parent: String) {
        val properties = mutableListOf<SchemaElement>()
        for (property in mapping.listOrNull("properties") ?: return) {
            property as? SchemaLiteral ?: error("Expected string property id ${property.path}")
            val propertyId = propertyIds.getOrElse("$parent|${property.string}") { property.string }
            properties.add(SchemaLiteral(propertyId))
        }
        mapping["properties"] = properties
    }

    private fun prettify(
        schema: SchemaMap,
        key: String,
        name: (SchemaMap) -> String? = { it.stringOrNull("name") },
        transform: (String, SchemaMap) -> Unit = { _, _ -> }
    ): Pair<MutableMap<String, SchemaElement>, Map<String, String>> {
        val elements = mutableMapOf<String, SchemaElement>()
        val ids = mutableMapOf<String, String>()
        for ((id, element) in schema.mapOfMapsOrNull(key) ?: return Pair(mutableMapOf(), mutableMapOf())) {
            transform(id, element)
            var name = name(element)
            if (name == null) {
                elements[id] = element
                continue
            }
            if (elements.containsKey(name)) {
                // Attempt name with number suffix
                for (i in 1 until 10) {
                    if (!elements.containsKey("${name}$i")) {
                        name = "${name}$i"
                        break
                    }
                }
                // Otherwise fall back to id
                if (name == null || elements.containsKey(name)) {
                    elements[id] = element
                    continue
                }
            }
            element.remove("name")
            elements[name] = element
            ids[id] = name
        }
        return Pair(elements, ids)
    }
}
