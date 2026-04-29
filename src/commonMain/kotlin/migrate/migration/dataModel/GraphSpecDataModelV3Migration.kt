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
import codec.schema.SchemaNull
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
        val constraints = mutableListOf<SchemaMap>()
        val indexes = mutableListOf<SchemaMap>()
        val nodeData = convertNodes(schema, constraints, indexes)
        val relData = convertRelationships(schema, constraints, indexes)
        return schemaMapOf(
            "version" to "3.0.0",
            "dataModel" to schemaMapOf(
                "version" to "3.0.0",
                "graphSchemaRepresentation" to schemaMapOf(
                    "version" to "1.0.0",
                    "graphSchema" to schemaMapOf(
                        "nodeLabels" toNotEmpty nodeData?.labelsMap,
                        "relationshipTypes" toNotEmpty relData?.typesMap,
                        "nodeObjectTypes" toNotEmpty nodeData?.objectTypes,
                        "relationshipObjectTypes" toNotEmpty relData?.objectTypes,
                        "constraints" to constraints,
                        "indexes" to indexes
                    )
                ),
                "graphMappingRepresentation" to convertGraphMapping(schema),
                "graphSchemaExtensionsRepresentation" to convertExtensions(schema)
            ),
            "visualisation" toNotEmpty convertVisualisation(schema)
        )
    }

    private fun convertGraphMapping(schema: SchemaMap): SchemaMap {
        val relationships = schema.mapOfMapsOrNull("relationships").orEmpty()
        val mappings = schema.listOfMapsOrNull("mappings").orEmpty()
        val (nodeMappings, relationshipMappings) = mappings.mapNotNull { mapping ->
            when {
                mapping.containsKey("node") -> "node" to schemaMapOf(
                    "node" to refOf(mapping.string("node")),
                    "tableName" to mapping.literal("table"),
                    "propertyMappings" to convertPropertyMappings(mapping.mapOfMapsOrNull("properties"))
                )
                mapping.containsKey("relationship") -> {
                    val relId = findRelationshipId(relationships, mapping) ?: return@mapNotNull null
                    "rel" to schemaMapOf(
                        "relationship" to refOf(relId),
                        "tableName" to mapping.literal("table"),
                        "fromMappings" toNotEmpty convertEntityMap(mapping.map("from").mapOfMapsOrNull("properties")),
                        "toMappings" toNotEmpty convertEntityMap(mapping.map("to").mapOfMapsOrNull("properties")),
                        "propertyMappings" to convertPropertyMappings(mapping.mapOfMapsOrNull("properties"))
                    )
                }
                else -> error("Invalid mapping type, must be node or relationship.")
            }
        }.partition { it.first == "node" }
        return schemaMapOf(
            "dataSourceSchema" toNotEmpty convertSourceSchema(schema.mapOfMapsOrNull("tables")),
            "nodeMappings" toNotEmpty nodeMappings.map { it.second },
            "relationshipMappings" toNotEmpty relationshipMappings.map { it.second }
        )
    }

    internal fun convertExtensions(schema: SchemaMap): SchemaMap? {
        val nodes = schema.mapOfMapsOrNull("nodes") ?: return null
        val nodeKeyProperties = mutableListOf<SchemaMap>()
        for ((nodeId, node) in nodes) {
            val properties = node.mapOfMapsOrNull("properties") ?: continue
            val keyProperties = mutableSetOf<String>()
            for ((propertyId, property) in properties) {
                val nullable = property.boolOrNull("nullable") == true
                val unique = property.boolOrNull("unique") == true
                if (!nullable && unique) {
                    keyProperties.add(propertyId)
                }
            }
            if (keyProperties.isNotEmpty()) {
                nodeKeyProperties.add(
                    schemaMapOf(
                        "node" to refOf(nodeId),
                        "keyProperties" to keyProperties.map { id ->
                            refOf(id)
                        }
                    )
                )
            }
        }
        return schemaMapOf(
            "nodeKeyProperties" toNotEmpty nodeKeyProperties
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
        val typesMap: List<SchemaMap>,
        val objectTypes: List<SchemaMap>,
        val constraints: List<SchemaMap>,
        val indexes: List<SchemaMap>
    )

    private fun convertRelationships(
        schema: SchemaMap,
        constraints: MutableList<SchemaMap>,
        indexes: MutableList<SchemaMap>
    ): RelationshipData? {
        val relationships = schema.mapOfMapsOrNull("relationships") ?: return null
        val relationTypes = mutableListOf<SchemaMap>()
        val relationshipObjectTypes = mutableListOf<SchemaMap>()
        for ((relId, rel) in relationships) {
            val typeToken = rel.string("type")
//            var typeId = relTypes[typeToken]
//            if (typeId == null) {
            // TODO if we look-up existing tokens then all relationships get combined
            //      if do don't then joint relationships always get separated
            val typeId = "rt:${relationTypes.size}"
//                relTypes[typeToken] = typeId
            relationTypes.add(
                schemaMapOf(
                    "\$id" to typeId,
                    "token" to typeToken,
                    "properties" to convertProperties(rel.mapOfMapsOrNull("properties"))
                )
            )
//            }

            relationshipObjectTypes.add(
                schemaMapOf(
                    "\$id" to relId,
                    "type" to refOf(typeId),
                    "from" to refOf(rel.map("from").string("node")),
                    "to" to refOf(rel.map("to").string("node"))
                )
            )

            constraints.addAll(
                convertElements(
                    id = "c:${constraints.size}",
                    elements = rel.mapOfMapsOrNull("constraints"),
                    entityType = "relationship",
                    refId = typeId,
                    typeKey = "constraintType",
                    typeTransform = ::constraintType
                )
            )
            indexes.addAll(
                convertElements(
                    id = "i:${indexes.size}",
                    elements = rel.mapOfMapsOrNull("indexes"),
                    entityType = "relationship",
                    refId = typeId,
                    typeKey = "indexType",
                    typeTransform = ::indexType
                )
            )
        }
        return RelationshipData(
            typesMap = relationTypes,
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

    private fun convertNodes(
        schema: SchemaMap,
        constraints: MutableList<SchemaMap>,
        indexes: MutableList<SchemaMap>
    ): NodeData? {
        val nodes = schema.mapOfMapsOrNull("nodes") ?: return null
        val nodeLabelsMap = mutableMapOf<String, String>()
        val nodeLabels = mutableListOf<SchemaMap>()
        val nodeObjectTypes = mutableListOf<SchemaMap>()
        for ((nodeId, node) in nodes) {
            val labelsInfo = node.map("labels")
            val primaryLabel = labelsInfo.string("identifier")
            val impliedLabels = labelsInfo.listOrNull("implied")?.map { it.toString() } ?: emptyList()
            val optionalLabels = labelsInfo.listOrNull("optional")?.map { it.toString() } ?: emptyList()
            val allLabels = listOf(primaryLabel) + impliedLabels + optionalLabels

            var primaryLabelId = "nl:null"
            val labelRefs = allLabels.map { label ->
                var labelId = nodeLabelsMap[label]
                if (labelId == null) {
                    labelId = "nl:${nodeLabels.size}"
                    nodeLabelsMap[label] = labelId
                    nodeLabels.add(
                        schemaMapOf(
                            "\$id" to labelId,
                            "token" to label,
                            "properties" to convertProperties(node.mapOfMapsOrNull("properties"))
                        )
                    )
                }
                if (label == primaryLabel) {
                    primaryLabelId = labelId
                }
                refOf(labelId)
            }
            nodeObjectTypes.add(
                schemaMapOf(
                    "\$id" to nodeId,
                    "labels" to labelRefs
                )
            )
            constraints.addAll(
                convertElements(
                    id = "c:${constraints.size}",
                    elements = node.mapOfMapsOrNull("constraints"),
                    entityType = "node",
                    refId = primaryLabelId,
                    typeKey = "constraintType",
                    typeTransform = ::constraintType
                )
            )
            indexes.addAll(
                convertElements(
                    id = "i:${indexes.size}",
                    elements = node.mapOfMapsOrNull("indexes"),
                    entityType = "node",
                    refId = primaryLabelId,
                    typeKey = "indexType",
                    typeTransform = ::indexType
                )
            )
        }
        return NodeData(nodeLabels, nodeObjectTypes, constraints, indexes)
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
                    "primaryKeys" to table.listOrNull("primaryKeys"),
                    "foreignKeys" to convertForeignKeys(table.mapOfMapsOrNull("foreignKeys"))
                )
            )
        }
        return schemaMapOf(
            "type" to sourceType,
            "tableSchemas" toNotEmpty tableSchemas
        )
    }

    internal fun convertVisualisation(schema: SchemaMap): SchemaMap {
        val display = schema.mapOrNull("display")?.mapOfMaps("nodes")
        return schemaMapOf(
            "nodes" to display?.map { (id, pos) ->
                schemaMapOf(
                    "id" to id,
                    "position" to schemaMapOf(
                        "x" to pos.literal("x"),
                        "y" to pos.literal("y")
                    )
                )
            }
        )
    }

    internal fun convertProperties(properties: Map<String, SchemaMap>?): List<SchemaMap> {
        if (properties.isNullOrEmpty()) return emptyList()
        return properties.map { (propId, prop) ->
            schemaMapOf(
                "\$id" to propId,
                "token" to prop.literalOrNull("name"),
                "type" to schemaMapOf(
                    "type" to propertyType(prop.string("type"))
                ),
                "nullable" to prop.literalOrNull("nullable")
            )
        }
    }

    internal fun convertElements(
        id: String,
        elements: Map<String, SchemaMap>?,
        entityType: String,
        refId: String,
        typeKey: String,
        typeTransform: (String) -> String
    ): List<SchemaMap> {
        if (elements.isNullOrEmpty()) {
            return emptyList()
        }
        return elements.map { (name, element) ->
            val properties = element.listOrNull("properties")?.map { propId ->
                refOf((propId as SchemaLiteral).string)
            } ?: emptyList()
            schemaMapOf(
                "\$id" to id,
                "name" to name,
                typeKey to typeTransform(element.string("type")),
                "entityType" to entityType,
                "nodeLabel" to if (entityType == "node") refOf(refId) else SchemaNull(),
                "properties" to properties,
                "relationshipType" to if (entityType == "relationship") refOf(refId) else SchemaNull()
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
                    val propertyType = propertyType(it.string)
                    if (propertyType == null) {
                        SchemaNull()
                    } else {
                        schemaMapOf("type" to propertyType)
                    }
                },
                "supportedTypes" to field.listOrNull("supported")?.map {
                    schemaMapOf("type" to propertyType((it as SchemaLiteral).string))
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
                    "referencedField" to (referencedFields.getOrNull(i) ?: referencedFields.last())
                    // TODO set or list with duplicates?
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
                "fieldName" to propDef.literal("field"),
                "property" to refOf(propId)
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
        private fun propertyType(string: String?): String? = when (string?.uppercase()) {
            "LOCAL DATETIME" -> "localdatetime"
            "ZONED DATETIME" -> "datetime"
            "STRING" -> "string"
            "INTEGER" -> "integer"
            "FLOAT" -> "float"
            "DATE" -> "date"
            "BOOLEAN" -> "boolean"
            "ANY" -> null
            "LOCAL TIME" -> "localtime"
            "ZONED TIME" -> "time"
            "POINT" -> "point"
            else -> string
        }

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
