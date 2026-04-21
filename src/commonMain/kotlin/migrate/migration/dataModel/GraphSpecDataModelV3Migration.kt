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
package migrate.migration.dataModel

import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.schemaMapOf
import codec.schema.toNotEmpty
import migrate.Migration
import model.Type
import model.Version
import net.pearx.kasechange.toCamelCase
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class GraphSpecDataModelV3Migration :
    Migration(
        fromType = Type.GRAPH_SPEC,
        from = Version.LATEST,
        toType = Type.DATA_MODEL,
        to = Version.DATA_MODEL_V30
    ) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val nodeData = convertNodes(schema)
        val relData = convertRelationships(schema)
        return schemaMapOf(
            "version" to (schema.literalOrNull("version") ?: "3.0"),
            "graphSchemaRepresentation" to schemaMapOf(
                "graphSchema" to schemaMapOf(
                    "nodeLabels" toNotEmpty nodeData?.labelsMap,
                    "relationshipTypes" toNotEmpty relData?.typesMap,
                    "nodeObjectTypes" toNotEmpty nodeData?.objectTypes,
                    "relationshipObjectTypes" toNotEmpty relData?.objectTypes,
                    "constraints" toNotEmpty (nodeData?.constraints.orEmpty() + relData?.constraints.orEmpty()),
                    "indexes" toNotEmpty (nodeData?.indexes.orEmpty() + relData?.indexes.orEmpty())
                )
            ),
            "graphMappingRepresentation" to convertGraphMapping(schema),
            "visualisation" toNotEmpty convertVisualisation(schema.mapOrNull("display")?.mapOfMaps("nodes"))
        )
    }

    private fun convertGraphMapping(schema: SchemaMap): SchemaMap {
        val relationships = schema.mapOfMapsOrNull("relationships").orEmpty()
        val mappings = schema.listOfMapsOrNull("mappings").orEmpty()
        val (nodeMappings, relationshipMappings) = mappings.mapNotNull { mapping ->
            when {
                mapping.containsKey("node") -> "node" to schemaMapOf(
                    "node" to schemaMapOf("\$ref" to "#${mapping.string("node")}"),
                    "tableName" to mapping.literal("table"),
                    "propertyMappings" toNotEmpty convertPropertyMappings(mapping.mapOfMapsOrNull("properties"))
                )
                mapping.containsKey("relationship") -> {
                    val relId = findRelationshipId(relationships, mapping) ?: return@mapNotNull null
                    "rel" to schemaMapOf(
                        "relationship" to schemaMapOf("\$ref" to "#$relId"),
                        "tableName" to mapping.literal("table"),
                        "propertyMappings" toNotEmpty convertPropertyMappings(mapping.mapOfMapsOrNull("properties")),
                        "fromMappings" toNotEmpty convertEntityMap(mapping.map("from").mapOfMapsOrNull("properties")),
                        "toMappings" toNotEmpty convertEntityMap(mapping.map("to").mapOfMapsOrNull("properties"))
                    )
                }
                else -> null
            }
        }.partition { it.first == "node" }
        return schemaMapOf(
            "dataSourceSchema" toNotEmpty convertSourceSchema(schema.mapOfMapsOrNull("tables")),
            "nodeMappings" toNotEmpty nodeMappings.map { it.second },
            "relationshipMappings" toNotEmpty relationshipMappings.map { it.second }
        )
    }

    /**
     * Recovers the lost relationship ObjectType reference matching the unique combo of (token, from, to)
     */
    private fun findRelationshipId(relationships: Map<String, SchemaMap>, mapping: SchemaMap): String? {
        val token = mapping.string("relationship")
        val fromNode = mapping.map("from").string("node")
        val toNode = mapping.map("to").string("node")
        return relationships.entries.firstOrNull { (_, rel) ->
            rel.string("type") == token &&
                rel.map("from").string("node") == fromNode &&
                rel.map("to").string("node") == toNode
        }?.key
    }

    private data class RelationshipData(
        val typesMap: List<Map<String, Any?>>,
        val objectTypes: List<SchemaMap>,
        val constraints: List<SchemaMap>,
        val indexes: List<SchemaMap>
    )

    private fun convertRelationships(schema: SchemaMap): RelationshipData? {
        val constraints = mutableListOf<SchemaMap>()
        val indexes = mutableListOf<SchemaMap>()
        val relationships = schema.mapOfMapsOrNull("relationships") ?: return null
        val relTypesMap = mutableMapOf<String, MutableMap<String, Any?>>()
        val relationshipObjectTypes = mutableListOf<SchemaMap>()
        for ((relId, rel) in relationships) {
            val typeToken = rel.string("type")
            val typeId = "rt:$typeToken"
            if (typeId !in relTypesMap) {
                relTypesMap[typeId] = mutableMapOf(
                    "\$id" to typeId,
                    "token" to typeToken,
                    "properties" to mutableListOf<SchemaMap>()
                )
            }

            val existingProps = relTypesMap[typeId]!!["properties"] as MutableList<SchemaMap>
            val currentPropIds = existingProps.map { it.id() }.toSet()

            val relProps = convertProperties(rel.mapOfMapsOrNull("properties"))
            existingProps.addAll(relProps.filter { it.id() !in currentPropIds })

            relationshipObjectTypes.add(
                schemaMapOf(
                    "\$id" to relId,
                    "type" to schemaMapOf("\$ref" to "#$typeId"),
                    "from" to schemaMapOf("\$ref" to "#${rel.map("from").string("node")}"),
                    "to" to schemaMapOf("\$ref" to "#${rel.map("to").string("node")}")
                )
            )

            constraints.addAll(
                convertElements(
                    elements = rel.mapOfMapsOrNull("constraints"),
                    entityType = "relationship",
                    refKey = "relationshipType",
                    refId = typeId,
                    typeKey = "constraintType",
                    typeTransform = ::constraintType
                )
            )
            indexes.addAll(
                convertElements(
                    elements = rel.mapOfMapsOrNull("indexes"),
                    entityType = "relationship",
                    refKey = "relationshipType",
                    refId = typeId,
                    typeKey = "indexType",
                    typeTransform = ::indexType
                )
            )
        }
        return RelationshipData(
            typesMap = relTypesMap.values.map { it.toMap() },
            objectTypes = relationshipObjectTypes,
            constraints = constraints,
            indexes = indexes
        )
    }

    private data class NodeData(
        val labelsMap: List<Map<String, Any?>>,
        val objectTypes: List<SchemaMap>,
        val constraints: List<SchemaMap>,
        val indexes: List<SchemaMap>
    )

    private fun convertNodes(schema: SchemaMap): NodeData? {
        val constraints = mutableListOf<SchemaMap>()
        val indexes = mutableListOf<SchemaMap>()
        val nodes = schema.mapOfMapsOrNull("nodes") ?: return null
        val nodeLabelsMap = mutableMapOf<String, MutableMap<String, Any?>>()
        val nodeObjectTypes = mutableListOf<SchemaMap>()
        for ((nodeId, node) in nodes) {
            val labelsInfo = node.map("labels")
            val primaryLabel = labelsInfo.string("identifier")
            val impliedLabels = labelsInfo.listOrNull("implied")?.map { it.toString() } ?: emptyList()
            val allLabels = listOf(primaryLabel) + impliedLabels

            val labelRefs = allLabels.map { label ->
                val labelId = "nl:$label"
                if (labelId !in nodeLabelsMap) {
                    nodeLabelsMap[labelId] = mutableMapOf(
                        "\$id" to labelId,
                        "token" to label,
                        "properties" to mutableListOf<SchemaMap>()
                    )
                }
                schemaMapOf("\$ref" to "#$labelId")
            }

            // Map properties back to the primary label
            val primaryLabelId = "nl:$primaryLabel"
            val existingProps = nodeLabelsMap[primaryLabelId]!!["properties"] as MutableList<SchemaMap>
            val currentPropIds = existingProps.map { it.id() }.toSet()

            val nodeProps = convertProperties(node.mapOfMapsOrNull("properties"))
            existingProps.addAll(nodeProps.filter { it.id() !in currentPropIds })

            nodeObjectTypes.add(
                schemaMapOf(
                    "\$id" to nodeId,
                    "labels" to labelRefs
                )
            )

            constraints.addAll(
                convertElements(
                    elements = node.mapOfMapsOrNull("constraints"),
                    entityType = "node",
                    refKey = "nodeLabel",
                    refId = primaryLabelId,
                    typeKey = "constraintType",
                    typeTransform = ::constraintType
                )
            )
            indexes.addAll(
                convertElements(
                    elements = node.mapOfMapsOrNull("indexes"),
                    entityType = "node",
                    refKey = "nodeLabel",
                    refId = primaryLabelId,
                    typeKey = "indexType",
                    typeTransform = ::indexType
                )
            )
        }
        return NodeData(nodeLabelsMap.values.map { it.toMap() }, nodeObjectTypes, constraints, indexes)
    }

    private fun convertSourceSchema(tables: Map<String, SchemaMap>?): SchemaMap? {
        val tableSchemas = mutableListOf<SchemaMap>()
        var sourceType: Any? = null
        for ((tableName, table) in tables ?: return null) {
            if (sourceType == null) {
                sourceType = table.literalOrNull("source")
            }
            tableSchemas.add(
                schemaMapOf(
                    "name" to tableName,
                    "fields" to convertFields(table.mapOfMapsOrNull("fields")),
                    "primaryKeys" toNotEmpty table.listOrNull("primaryKeys"),
                    "foreignKeys" toNotEmpty convertForeignKeys(table.mapOfMapsOrNull("foreignKeys"))
                )
            )
        }
        return schemaMapOf(
            "type" to sourceType,
            "tableSchemas" toNotEmpty tableSchemas
        )
    }

    internal fun convertVisualisation(display: Map<String, SchemaMap>?): SchemaMap? {
        if (display.isNullOrEmpty()) {
            return null
        }
        val nodes = display.map { (id, pos) ->
            schemaMapOf(
                "id" to id,
                "position" to schemaMapOf(
                    "x" to pos.literal("x"),
                    "y" to pos.literal("y")
                )
            )
        }
        return schemaMapOf("nodes" to nodes)
    }

    internal fun convertProperties(properties: Map<String, SchemaMap>?): List<SchemaMap> {
        if (properties.isNullOrEmpty()) return emptyList()
        return properties.map { (propId, prop) ->
            schemaMapOf(
                "\$id" to propId,
                "token" to prop.literalOrNull("name"),
                "type" to schemaMapOf(
                    "type" to prop.string("type").lowercase(),
                    "nullable" to prop.literalOrNull("nullable")
                )
            )
        }
    }

    internal fun convertElements(
        elements: Map<String, SchemaMap>?,
        entityType: String,
        refKey: String,
        refId: String,
        typeKey: String,
        typeTransform: (String) -> String
    ): List<SchemaMap> {
        if (elements.isNullOrEmpty()) {
            return emptyList()
        }
        return elements.map { (name, element) ->
            schemaMapOf(
                "name" to name,
                typeKey to typeTransform(element.string("type")),
                "entityType" to entityType,
                refKey to schemaMapOf("\$ref" to "#$refId"),
                "properties" to (
                    element.listOrNull("properties")?.map { propId ->
                        schemaMapOf("\$ref" to "#$propId")
                    } ?: emptyList()
                    )
            )
        }
    }

    internal fun convertFields(fields: Map<String, SchemaMap>?): List<SchemaMap> {
        if (fields.isNullOrEmpty()) {
            return emptyList()
        }
        return fields.values.map { field ->
            schemaMapOf(
                "name" to field.literalOrNull("name"),
                "rawType" to field.literalOrNull("type"),
                "size" to field.literalOrNull("size"),
                "recommendedType" to field.literalOrNull("suggested")?.let {
                    schemaMapOf("type" to it.string.toCamelCase())
                },
                "supportedTypes" to field.listOrNull("supported")?.map {
                    schemaMapOf("type" to (it as SchemaLiteral).string.toCamelCase())
                }
            )
        }
    }

    internal fun convertForeignKeys(foreignKeys: Map<String, SchemaMap>?): List<SchemaMap> {
        if (foreignKeys.isNullOrEmpty()) {
            return emptyList()
        }
        return foreignKeys.values.map { fk ->
            val fields = fk.list("fields").map { it.toString() }
            val references = fk.map("references")
            val referencedFields = references.list("fields").map { it.toString() }

            val fieldMaps = fields.indices.map { i ->
                schemaMapOf(
                    "field" to fields[i],
                    "referencedField" to referencedFields[i]
                )
            }

            schemaMapOf(
                "referencedTable" to references.literal("table"),
                "fields" to fieldMaps
            )
        }
    }

    internal fun convertPropertyMappings(properties: Map<String, SchemaMap>?): List<SchemaMap> {
        if (properties.isNullOrEmpty()) {
            return emptyList()
        }
        return properties.map { (propId, propDef) ->
            schemaMapOf(
                "property" to schemaMapOf("\$ref" to "#$propId"),
                "fieldName" to propDef.literal("field")
            )
        }
    }

    internal fun convertEntityMap(properties: Map<String, SchemaMap>?): Map<String, Any> {
        if (properties.isNullOrEmpty()) {
            return emptyMap()
        }
        return properties.entries.associate { (key, value) ->
            "#$key" to value.literal("field")
        }
    }

    companion object {
        private fun constraintType(name: String): String = when (name) {
            "UNIQUE" -> "uniqueness"
            "EXISTS" -> "propertyExistence"
            "TYPE" -> "propertyType"
            "KEY" -> "key"
            else -> name
        }

        private fun indexType(name: String): String = when (name) {
            "LOOKUP" -> "lookup"
            "RANGE" -> "range"
            "FULLTEXT" -> "fulltext"
            "POINT" -> "point"
            "TEXT" -> "text"
            "VECTOR" -> "vector"
            else -> name
        }
    }
}
