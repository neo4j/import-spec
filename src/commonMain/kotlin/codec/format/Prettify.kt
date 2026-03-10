package codec.format

import codec.schema.SchemaElement
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.schemaMapOf
import kotlin.collections.iterator

object Prettify {
    fun transform(schema: SchemaMap): SchemaMap {
        val nodePropertyIds = mutableMapOf<String, String>()
        val (nodes, nodeIds) = prettify(
            schema, "nodes",
            name = { it.stringOrNull("name") ?: (it.listOrNull("labels")?.firstOrNull() as? SchemaLiteral)?.string }
        ) { id, node ->
            val (properties, ids) = prettify(node, "properties")
            nodePropertyIds.putAll(ids.map { "$id|${it.key}" to it.value })
            node["properties"] = SchemaMap(properties)
        }
        schema["nodes"] = SchemaMap(nodes)
        val relationshipPropertyIds = mutableMapOf<String, String>()
        val (relationships, relationshipIds) = prettify(
            schema, "relationships",
            name = { it.stringOrNull("type") }
        ) { _, relationship ->
            val type = relationship.string("type")
            val from = relationship.string("from")
            relationship["from"] = nodeIds.getOrElse(from) { from }
            val to = relationship.string("to")
            relationship["to"] = nodeIds.getOrElse(to) { to }
            val (properties, ids) = prettify(relationship, "properties")
            relationshipPropertyIds.putAll(ids.map { "$type|${it.key}" to it.value })
            relationship["properties"] = SchemaMap(properties)
        }
        schema["relationships"] = SchemaMap(relationships)
        val mappings = schema.listOfMapsOrNull("mappings")
        if (mappings != null) {
            val newMappings = mutableListOf<SchemaElement>()
            for (mapping in mappings) {
                val node = mapping.stringOrNull("node")
                val type = mapping.stringOrNull("type")
                if (node != null) {
                    mapping["node"] = SchemaLiteral(nodeIds.getOrElse(node) { node })
                    updateProperties(mapping, nodePropertyIds, node)
                } else if (type != null) {
                    mapping["type"] = SchemaLiteral(relationshipIds.getOrElse(type) { type })
                    println(relationshipPropertyIds)
                    println("Type $type")
                    updateProperties(mapping, relationshipPropertyIds, type)
                    val from = mapping.map("from")
                    val fromNode = from.string("node")
                    from["node"] = SchemaLiteral(nodeIds.getOrElse(fromNode) { fromNode })
                    updateProperties(from, nodePropertyIds, fromNode)
                    val to = mapping.map("to")
                    val toNode = to.string("node")
                    to["node"] = SchemaLiteral(nodeIds.getOrElse(toNode) { toNode })
                    updateProperties(to, nodePropertyIds, toNode)
                }
                newMappings.add(mapping)
            }
            schema["mappings"] = SchemaList(newMappings)
        }
        if (schema.listOrNull("mappings")?.isEmpty() == true) {
            schema.remove("mappings")
        }
        if (schema.mapOrNull("tables")?.isEmpty() == true) {
            schema.remove("tables")
        }
        return schema
    }

    private fun updateProperties(mapping: SchemaMap, propertyIds: MutableMap<String, String>, parent: String) {
        val properties = mutableMapOf<String, SchemaElement>()
        for ((id, property) in mapping.mapOrNull("properties") ?: return) {
            val propertyId = propertyIds.getOrElse("$parent|${id}") { id }
            properties[propertyId] = property
        }
        mapping["properties"] = SchemaMap(properties)
    }

    private fun prettify(
        schema: SchemaMap,
        key: String,
        name: (SchemaMap) -> String? = { it.stringOrNull("name") },
        transform: (String, SchemaMap) -> Unit = { _, _ -> }
    ): Pair<MutableMap<String, SchemaElement>, Map<String, String>> {
        val elements = mutableMapOf<String, SchemaElement>()
        val ids = mutableMapOf<String, String>()
        for ((id, element) in schema.mapOfMaps(key)) {
            transform(id, element)
            val name = name(element)
            if (name == null || elements.containsKey(name)) {
                elements[id] = element
                continue
            }
            element.remove("name")
            elements[name] = element
            ids[id] = name
        }
        return Pair(elements, ids)
    }
}
