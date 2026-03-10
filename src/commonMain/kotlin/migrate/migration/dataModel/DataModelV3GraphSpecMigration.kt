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
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.SchemaNull
import codec.schema.schemaListOf
import codec.schema.schemaMapOf
import migrate.Migration
import model.Version
import model.constraint.ConstraintType

class DataModelV3GraphSpecMigration : Migration(Version.DATA_MODEL_V30, Version.LATEST) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        if (schema.containsKey("dataModel")) {
            val model = schema.map("dataModel")
            schema.remove("dataModel")
            schema.remove("version")
            model.putAll(schema)
            return migrate(model)
        }
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val nodes = migrateNodes(graphSchema)
        val relationships = migrateRelationships(graphSchema)
        val mappings = migrateMappings(schema)
        val tables = migrateTables(schema)
        val output = schemaMapOf(
            "version" to schema.literal("version"),
            "nodes" to SchemaMap(nodes),
            "relationships" to SchemaMap(relationships)
        )
        if (tables.isNotEmpty()) {
            output["tables"] = SchemaMap(tables)
        }
        if (mappings.isNotEmpty()) {
            output["mappings"] = SchemaList(mappings)
        }
        return output
    }

    private fun constraints(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val nodeConstraints = mutableMapOf<String, SchemaElement>()
        val constraints = schema.listOfMapsOrNull("constraints") ?: return mutableMapOf()
        for (constraint in constraints) {
            constraint.string("\$id")
            val name = constraint.string("name")
            val type = constraint.string("constraintType")
            val properties = constraint.mapOrNull("properties")
            val entity = constraint.string("entityType")
            if (entity == "node") {
                val nodeLabel = constraint.map("nodeLabel").string("\$ref").removePrefix("#")
                val constraintType = when (type) {
                    // TODO correct values for these
                    "uniqueness" -> ConstraintType.UNIQUE
                    "propertyExistence" -> ConstraintType.EXISTS
                    else -> error("Unknown constraint type: $type at ${constraint.path}.$name")
                }
                nodeConstraints[nodeLabel] = schemaMapOf(
                    "type" to SchemaLiteral(constraintType)
                )
            } else if (entity == "relationship") {
                val relationship = constraint.string("relationship")
            }
        }
        return mutableMapOf()
    }

    private fun migrateNodes(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val nodes = mutableMapOf<String, SchemaElement>()
        val nodeLabels = schema.listOfMaps("nodeLabels")
        for (nodeObject in schema.listOfMaps("nodeObjectTypes")) {
            val nodeRef = nodeObject.string("\$id")
            val labelRefs = nodeObject.listOfMaps("labels").map { it.string("\$ref").removePrefix("#") }
            val labels = labelRefs.map { labelRef ->
                nodeLabels.firstOrNull { it.string("\$id") == labelRef } ?: error("Label $labelRef not found")
            }
            val labelTokens = labels.map { it.string("token") }
            val properties = mutableMapOf<String, SchemaElement>()
            for (label in labels) {
                convertProperties(label, properties)
            }
            val node = schemaMapOf(
                "labels" to schemaListOf(*labelTokens.toTypedArray()),
                "properties" to SchemaMap(properties)
            )
            val primaryLabel = labelTokens.firstOrNull()
            if (primaryLabel != null) {
                node["name"] = SchemaLiteral(primaryLabel)
            }
            nodes[nodeRef] = node
        }
        return nodes
    }

    private fun migrateRelationships(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val relationships = mutableMapOf<String, SchemaElement>()
        val relationshipTypes = schema.listOfMaps("relationshipTypes")
        for (objectType in schema
            .listOfMaps("relationshipObjectTypes")) {
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").string("\$ref").removePrefix("#")
            val toRef = objectType.map("to").string("\$ref").removePrefix("#")

            val relationshipType = relationshipTypes
                .firstOrNull { it.string("\$id") == typeRef }
                ?: error("RelationshipType $typeRef not found")
            val token = relationshipType.literal("token")
            val properties = mutableMapOf<String, SchemaElement>()
            convertProperties(relationshipType, properties)
            val relationship = schemaMapOf(
                "type" to token,
                "from" to SchemaLiteral(fromRef),
                "to" to SchemaLiteral(toRef),
                "properties" to SchemaMap(properties)
            )
            relationships[ref] = relationship
        }
        return relationships
    }

    private fun convertProperties(label: SchemaMap, properties: MutableMap<String, SchemaElement>) {
        for (property in label.listOfMaps("properties")) {
            val ref = property.string("\$id")
            val token = property.literalOrNull("token")
            val typeObj = property.map("type") // TODO arrays
            val type = typeObj.string("type").uppercase()
            // TODO constraints
            val map = schemaMapOf(
                "name" to (token ?: SchemaNull),
                "type" to SchemaLiteral(type)
            )
            val nullable = typeObj.literalOrNull("nullable")
            if (nullable != null) {
                map["nullable"] = nullable
            }
            properties[ref] = map
        }
    }

    private fun migrateTables(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val tables = mutableMapOf<String, SchemaElement>()
        val sourceSchema = schema.map("graphMappingRepresentation").map("dataSourceSchema")
        val sourceType = sourceSchema.literalOrNull("type")
        for (table in sourceSchema.listOfMaps("tableSchemas")) {
            val fields = mutableMapOf<String, SchemaElement>()
            val name = table.string("name")
            for (field in table.listOfMaps("fields")) {
                val name = field.literal("name")
                val rawType = field.literal("rawType")
                val size = field.literal("size")
                val recommendedObject = field.map("recommendedType")
                val recommendedType = recommendedObject.string("type")
                val supportedTypes = field.listOfMaps("supportedTypes")
                    .map { it.string("type").uppercase() } // FIXME array types
                fields[name.string] = schemaMapOf(
                    "name" to name,
                    "rawType" to rawType,
                    "size" to size,
                    "suggested" to SchemaLiteral(recommendedType.uppercase()),
                    "supported" to schemaListOf(*supportedTypes.toTypedArray())
                )
            }
            val primaryKeys = table.list("primaryKeys")
            val foreignKeys = mutableMapOf<String, SchemaElement>()
            for (foreignKey in table.listOfMaps("foreignKeys")) {
                val tableRef = foreignKey.literal("referencedTable")
                val fields = mutableListOf<SchemaElement>()
                val referencedFields = mutableListOf<SchemaElement>()
                for (field in foreignKey.listOfMaps("fields")) {
                    fields.add(field.literal("field"))
                    referencedFields.add(field.literal("referencedField"))
                }
                foreignKeys["foreignKey${foreignKeys.size + 1}"] = schemaMapOf(
                    "fields" to SchemaList(fields),
                    "references" to schemaMapOf(
                        "table" to tableRef,
                        "fields" to SchemaList(referencedFields)
                    )
                )
            }
            tables[name] = schemaMapOf(
                "source" to (sourceType ?: SchemaNull),
                "fields" to SchemaMap(fields),
                "primaryKeys" to primaryKeys,
                "foreignKeys" to SchemaMap(foreignKeys)
            )
        }
        return tables
    }

    private fun migrateMappings(schema: SchemaMap): MutableList<SchemaElement> {
        val list = mutableListOf<SchemaElement>()
        nodeMappings(schema, list)
        relationshipMappings(schema, list)
        return list
    }

    private fun relationshipMappings(schema: SchemaMap, list: MutableList<SchemaElement>) {
        val relationships = mutableMapOf<String, Triple<String, String, String>>()
        val objectTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMapsOrNull("relationshipObjectTypes") ?: return
        for (objectType in objectTypes) {
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").string("\$ref").removePrefix("#")
            val toRef = objectType.map("to").string("\$ref").removePrefix("#")
            relationships[ref] = Triple(fromRef, typeRef, toRef)
        }

        val relationshipTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMapsOrNull("relationshipTypes") ?: return
        val relationshipMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("relationshipMappings") ?: return
        for (relationshipMapping in relationshipMappings) {
            val ref = relationshipMapping.map("relationship").string("\$ref").removePrefix("#")
            val (fromRef, typeRef, toRef) = relationships[ref] ?: error("Relationship $ref not found")

            val tableName = relationshipMapping.literal("tableName")
            val fromMappings = mutableMapOf<String, SchemaElement>()
            for ((key, value) in relationshipMapping.map("fromMappings")) {
                fromMappings[key.removePrefix("#")] = schemaMapOf("field" to value as SchemaLiteral)
            }
            val toMappings = mutableMapOf<String, SchemaElement>()
            for ((key, value) in relationshipMapping.map("toMappings")) {
                toMappings[key.removePrefix("#")] = schemaMapOf("field" to value as SchemaLiteral)
            }
            val type = relationshipTypes
                .firstOrNull { it.string("\$id") == typeRef }
                ?: error("Relationship type $typeRef not found")
            val token = type.string("token")
            val propertyMappings = relationshipMapping.listOfMaps("propertyMappings")
            val properties = migratePropertyMappings(propertyMappings)
            // ref is lost, and we know type isn't unique; so we are assuming type + property id are unique
            list.add(
                schemaMapOf(
                    "type" to SchemaLiteral(token),
                    "from" to schemaMapOf(
                        "node" to SchemaLiteral(fromRef),
                        "properties" to SchemaMap(fromMappings)
                    ),
                    "to" to schemaMapOf(
                        "node" to SchemaLiteral(toRef),
                        "properties" to SchemaMap(toMappings)
                    ),
                    "table" to tableName,
                    "properties" to SchemaMap(properties)
                )
            )
        }
    }

    private fun nodeMappings(schema: SchemaMap, list: MutableList<SchemaElement>) {
        val nodeMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("nodeMappings") ?: return
        for (nodeMapping in nodeMappings) {
            val ref = nodeMapping.map("node").string("\$ref").removePrefix("#")
            val tableName = nodeMapping.literal("tableName")
            val propertyMappings = nodeMapping.listOfMaps("propertyMappings")
            val properties = migratePropertyMappings(propertyMappings)
            list.add(
                schemaMapOf(
                    "node" to SchemaLiteral(ref),
                    "table" to tableName,
                    "properties" to SchemaMap(properties)
                )
            )
        }
    }

    private fun migratePropertyMappings(elements: List<SchemaMap>): MutableMap<String, SchemaElement> {
        val properties = mutableMapOf<String, SchemaElement>()
        for (propertyMapping in elements) {
            val propertyRef = propertyMapping.map("property").string("\$ref").removePrefix("#")
            val fieldName = propertyMapping.literal("fieldName")
            properties[propertyRef] = schemaMapOf(
                "field" to fieldName
            )
        }
        return properties
    }
}
