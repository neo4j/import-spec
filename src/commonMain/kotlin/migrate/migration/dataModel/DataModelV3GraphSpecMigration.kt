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

import codec.schema.SchemaElement
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.buildSchemaMap
import codec.schema.schemaMapOf
import codec.schema.toNotEmpty
import migrate.Migration
import migrate.migration.dataModel.DataModelV2V3Migration.Companion.unwrap
import model.Type
import model.Version
import model.constraint.ConstraintType
import model.index.IndexType

class DataModelV3GraphSpecMigration :
    Migration(
        fromType = Type.DATA_MODEL,
        from = Version.DATA_MODEL_V30,
        toType = Type.GRAPH_SPEC,
        to = Version.LATEST
    ) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val schema = unwrap(schema)
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val (nodeConstraints, relationshipConstraints) = gather(graphSchema, "constraints")
        val (nodeIndexes, relationshipIndexes) = gather(graphSchema, "indexes")
        val nodes = migrateNodes(graphSchema, nodeConstraints, nodeIndexes)
        visualisation(schema, nodes) // TODO
        return schemaMapOf(
            "version" to schema.literal("version"),
            "nodes" to nodes,
            "relationships" to migrateRelationships(graphSchema, relationshipConstraints, relationshipIndexes),
            "tables" toNotEmpty migrateTables(schema),
            "mappings" toNotEmpty nodeMappings(schema) + relationshipMappings(schema)
        )
    }

    private fun visualisation(schema: SchemaMap, nodes: MutableMap<String, SchemaElement>) {
        val visualisation = schema.remove("visualisation") as? SchemaMap ?: return
        for (vis in visualisation.listOfMaps("nodes")) {
            val ref = vis.string("id")
            val position = vis.map("position")
            val x = position.literal("x")
            val y = position.literal("y")
            val node = nodes[ref] as? SchemaMap ?: error("Unknown node $ref")
            val extensions = node.mapOrPut("extensions")
            extensions["x"] = x
            extensions["y"] = y
        }
        // TODO
    }

    private fun gather(
        schema: SchemaMap,
        key: String
    ): Pair<Map<String, List<SchemaMap>>, Map<String, List<SchemaMap>>> {
        val constraints = schema.listOfMapsOrNull(key) ?: return Pair(emptyMap(), emptyMap())
        val (nodes, rels) = constraints.partition { it.string("entityType") == "node" }
        val nodeConstraints = nodes.groupBy { it.ref("nodeLabel") }
        val relationshipConstraints = rels.groupBy { it.ref("relationshipType") }
        return Pair(nodeConstraints, relationshipConstraints)
    }

    private fun migrateNodes(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaElement> {
        val nodes = mutableMapOf<String, SchemaElement>()
        val nodeLabels = schema.listOfMaps("nodeLabels")
        for (nodeObject in schema.listOfMaps("nodeObjectTypes")) {
            val nodeRef = nodeObject.id()
            val labelRefs = nodeObject.listOfMaps("labels").map { it.ref() }
            val labels = labelRefs.map { labelRef ->
                nodeLabels.firstOrNull { it.id() == labelRef } ?: error("Label $labelRef not found")
            }
            val labelTokens = labels.map { it.string("token") }
            val labelRef = labelRefs.firstOrNull() // TODO loop all
            nodes[nodeRef] = schemaMapOf(
                "labels" to schemaMapOf(
                    "identifier" to labelTokens.first(),
                    "implied" toNotEmpty labelTokens.drop(1)
                ),
                "constraints" toNotEmpty convertConstraints(constraints, labelRef, labelTokens.first()),
                "indexes" toNotEmpty convertIndexes(indexes, labelRef, labelTokens.first()),
                "properties" toNotEmpty convertProperties(*labels.toTypedArray()),
                "name" to labelTokens.firstOrNull()
            )
        }
        return nodes
    }

    private fun convertIndexes(indexes: Map<String, List<SchemaMap>>, labelRef: String?, label: String): SchemaMap? {
        val indices = indexes[labelRef] ?: return null
        return buildSchemaMap {
            var i = 1
            for (index in indices) {
                val name = index.string("name") // node name?
                val key = if (containsKey(name)) "index${i++}" else name
                this[key] = schemaMapOf(
                    "type" to indexType(index).name,
                    "labels" to listOf(label),
                    "properties" to index.listOfMapsOrNull("properties")?.map { it.ref() }
                )
            }
        }
    }

    private fun indexType(index: SchemaMap): IndexType = when (val type = index.string("indexType")) {
        "lookup" -> IndexType.LOOKUP
        "range" -> IndexType.RANGE
        "fulltext" -> IndexType.FULLTEXT
        "point" -> IndexType.POINT
        "default", "text" -> IndexType.TEXT
        else -> error("Unknown index type: '$type' at ${index.path}.${index.string("name")}")
    }

    private fun convertConstraints(
        constraints: Map<String, List<SchemaMap>>,
        labelRef: String?,
        label: String
    ): SchemaMap? {
        val constraints = constraints[labelRef] ?: return null
        return buildSchemaMap {
            var i = 1
            for (constraint in constraints) {
                val properties = constraint.listOfMapsOrNull("properties")
                val constraintType = constraintType(constraint)
                if (properties != null && properties.size > 1 && constraintType == ConstraintType.TYPE) {
                    error("Type constraints not supported on multiple properties.")
                }
                val name = constraint.string("name") // node name?
                val key = if (containsKey(name)) "constraint${i++}" else name
                this[key] = schemaMapOf(
                    "type" to constraintType.name,
                    "label" to label,
                    "properties" toNotEmpty properties?.map { it.ref() }
                )
            }
        }
    }

    private fun constraintType(constraint: SchemaMap): ConstraintType =
        when (val type = constraint.string("constraintType")) {
            "uniqueness" -> ConstraintType.UNIQUE
            "propertyExistence" -> ConstraintType.EXISTS
            "propertyType" -> ConstraintType.TYPE
            "key" -> ConstraintType.KEY
            else -> error("Unknown constraint type: $type at ${constraint.path}.${constraint.string("name")}")
        }

    private fun migrateRelationships(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaElement> {
        val relationships = mutableMapOf<String, SchemaElement>()
        val relationshipTypes = schema.listOfMaps("relationshipTypes")
        for (objectType in schema.listOfMaps("relationshipObjectTypes")) {
            val typeRef = objectType.ref("type")
            val relationshipType = relationshipTypes
                .firstOrNull { it.id() == typeRef }
                ?: error("RelationshipType $typeRef not found")
            val token = relationshipType.literal("token")
            val id = objectType.id()
            relationships[id] = schemaMapOf(
                "type" to token,
                "from" to mapOf(
                    "node" to objectType.ref("from")
                ),
                "to" to mapOf(
                    "node" to objectType.ref("to")
                ),
                "properties" to convertProperties(relationshipType),
                "constraints" toNotEmpty convertConstraints(constraints, typeRef, token.string),
                "indexes" toNotEmpty convertIndexes(indexes, typeRef, token.string)
            )
        }
        return relationships
    }

    private fun convertProperties(vararg labels: SchemaMap): MutableMap<String, SchemaMap> {
        val properties = mutableMapOf<String, SchemaMap>()
        for (label in labels) {
            for (property in label.listOfMaps("properties")) {
                val id = property.id()
                val typeObj = property.map("type") // TODO arrays
                // TODO constraints
                properties[id] = schemaMapOf(
                    "name" to property.literalOrNull("token"),
                    "type" to typeObj.string("type").uppercase(),
                    "nullable" to typeObj.literalOrNull("nullable")
                )
            }
        }
        return properties
    }

    private fun migrateTables(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val tables = mutableMapOf<String, SchemaElement>()
        val sourceSchema = schema.map("graphMappingRepresentation").map("dataSourceSchema")
        val sourceType = sourceSchema.literalOrNull("type")
        for (table in sourceSchema.listOfMaps("tableSchemas")) {
            val name = table.string("name")
            tables[name] = schemaMapOf(
                "source" to sourceType,
                "fields" to migrateFields(table),
                "primaryKeys" toNotEmpty table.listOrNull("primaryKeys"),
                "foreignKeys" toNotEmpty migrateForeignKeys(table)
            )
        }
        return tables
    }

    private fun migrateForeignKeys(table: SchemaMap): MutableMap<String, SchemaElement>? {
        val foreign = table.listOfMapsOrNull("foreignKeys") ?: return null
        val foreignKeys = mutableMapOf<String, SchemaElement>()
        for (foreignKey in foreign) {
            val fields = mutableListOf<SchemaElement>()
            val referencedFields = mutableListOf<SchemaElement>()
            for (field in foreignKey.listOfMaps("fields")) {
                fields.add(field.literal("field"))
                referencedFields.add(field.literal("referencedField"))
            }
            foreignKeys["foreignKey${foreignKeys.size + 1}"] = schemaMapOf(
                "fields" to fields,
                "references" to mapOf(
                    "table" to foreignKey.literal("referencedTable"),
                    "fields" to referencedFields
                )
            )
        }
        return foreignKeys
    }

    private fun migrateFields(table: SchemaMap): MutableMap<String, SchemaElement> {
        val fields = mutableMapOf<String, SchemaElement>()
        for (field in table.listOfMaps("fields")) {
            val name = field.literal("name")
            fields[name.string] = schemaMapOf(
                "name" to name,
                "rawType" to field.literalOrNull("rawType"),
                "size" to field.literalOrNull("size"),
                "suggested" to field.mapOrNull("recommendedType")?.string("type")?.uppercase(),
                "supported" to field.listOfMapsOrNull("supportedTypes")?.map {
                    // FIXME array types
                    it.string("type").uppercase()
                }
            )
        }
        return fields
    }

    private fun relationshipMappings(schema: SchemaMap): List<SchemaElement> {
        val relationships = mutableMapOf<String, Triple<String, String, String>>()
        val objectTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMapsOrNull("relationshipObjectTypes") ?: return emptyList()
        for (objectType in objectTypes) {
            val ref = objectType.id()
            val typeRef = objectType.ref("type")
            val fromRef = objectType.ref("from")
            val toRef = objectType.ref("to")
            relationships[ref] = Triple(fromRef, typeRef, toRef)
        }

        val relationshipTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMapsOrNull("relationshipTypes") ?: return emptyList()
        val relationshipMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("relationshipMappings") ?: return emptyList()
        val mappings = mutableListOf<SchemaMap>()
        for (relationshipMapping in relationshipMappings) {
            val ref = relationshipMapping.ref("relationship")
            val (fromRef, typeRef, toRef) = relationships[ref] ?: error("Relationship $ref not found")
            val type = relationshipTypes
                .firstOrNull { it.id() == typeRef }
                ?: error("Relationship type $typeRef not found")
            // ref is lost, and we know type isn't unique; so we are assuming type + property id are unique
            mappings += schemaMapOf(
                "relationship" to type.string("token"),
                "from" to mapOf(
                    "node" to fromRef,
                    "properties" to relationshipMapping.map("fromMappings").map { (key, value) ->
                        key.removePrefix("#") to schemaMapOf("field" to value)
                    }.toMap()
                ),
                "to" to mapOf(
                    "node" to toRef,
                    "properties" to relationshipMapping.map("toMappings").map { (key, value) ->
                        key.removePrefix("#") to schemaMapOf("field" to value)
                    }.toMap()
                ),
                "table" to relationshipMapping.literal("tableName"),
                "properties" toNotEmpty migratePropertyMappings(relationshipMapping)
            )
        }
        return mappings
    }

    private fun nodeMappings(schema: SchemaMap): List<SchemaMap> {
        val nodeMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("nodeMappings") ?: return emptyList()
        val mappings = mutableListOf<SchemaMap>()
        for (nodeMapping in nodeMappings) {
            mappings += schemaMapOf(
                "node" to nodeMapping.ref("node"),
                "table" to nodeMapping.literal("tableName"),
                "properties" toNotEmpty migratePropertyMappings(nodeMapping)
            )
        }
        return mappings
    }

    private fun migratePropertyMappings(entity: SchemaMap) = entity
        .listOfMaps("propertyMappings")
        .associate { mapping ->
            mapping.ref("property") to mapOf(
                "field" to mapping.literal("fieldName")
            )
        }

    private fun SchemaMap.ref() = string("\$ref").removePrefix("#")

    private fun SchemaMap.ref(key: String) = map(key).ref()

    private fun SchemaMap.id() = string("\$id")
}
