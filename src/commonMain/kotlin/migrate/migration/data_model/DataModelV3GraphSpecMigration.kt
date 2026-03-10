package migrate.migration.data_model

import codec.schema.SchemaElement
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.SchemaNull
import codec.schema.schemaListOf
import codec.schema.schemaMapOf
import migrate.Migration
import model.Version

// TODO supported nested "dataModel"'s
class DataModelV3GraphSpecMigration : Migration(Version.DATA_MODEL_V30, Version.LATEST) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val nodes = migrateNodes(graphSchema)
        val relationships = migrateRelationships(graphSchema)
        val mappings = migrateMappings(schema) // TODO: Are mappings required in data model?
        val tables = migrateTables(schema)
        return schemaMapOf(
            "version" to schema.literal("version"),
            "nodes" to SchemaMap(nodes),
            "relationships" to SchemaMap(relationships),
            "tables" to SchemaMap(tables),
            "mappings" to SchemaList(mappings),
        )
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

    private fun convertProperties(
        label: SchemaMap,
        properties: MutableMap<String, SchemaElement>
    ) {
        for (property in label.listOfMaps("properties")) {
            val ref = property.string("\$id")
            val token = property.literalOrNull("token")
            val typeObj = property.map("type") // TODO arrays
            val type = typeObj.string("type").uppercase()
            // TODO constraints
            val map = schemaMapOf(
                "name" to (token ?: SchemaNull),
                "type" to SchemaLiteral(type),
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
                val supportedTypes = field.listOfMaps("supportedTypes").map { it.string("type").uppercase() } // FIXME array types
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
                        "fields" to SchemaList(referencedFields),
                    )
                )
            }
            tables[name] = schemaMapOf(
                "source" to (sourceType ?: SchemaNull),
                "fields" to SchemaMap(fields),
                "primaryKeys" to primaryKeys,
                "foreignKeys" to SchemaMap(foreignKeys),
            )
        }
        return tables
    }

    private fun migrateMappings(schema: SchemaMap): MutableList<SchemaElement> {
        val list = mutableListOf<SchemaElement>()
        // Node mappings
        for (nodeMapping in schema
            .map("graphMappingRepresentation")
            .listOfMaps("nodeMappings")) {
            val ref = nodeMapping.map("node").string("\$ref").removePrefix("#")
            val tableName = nodeMapping.literal("tableName")

            val properties = migratePropertyMappings(nodeMapping.listOfMaps("propertyMappings"))
            list.add(
                schemaMapOf(
                    "node" to SchemaLiteral(ref),
                    "table" to tableName,
                    "properties" to SchemaMap(properties),
                )
            )
        }

        // Relationship mappings
        val relationships = mutableMapOf<String, Triple<String, String, String>>()
        for (objectType in schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("relationshipObjectTypes")) {
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").string("\$ref").removePrefix("#")
            val toRef = objectType.map("to").string("\$ref").removePrefix("#")
            relationships[ref] = Triple(fromRef, typeRef, toRef)
        }

        val relationshipTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("relationshipTypes")
        for (relationshipMapping in schema
            .map("graphMappingRepresentation")
            .listOfMaps("relationshipMappings")) {
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
            val properties = migratePropertyMappings(relationshipMapping.listOfMaps("propertyMappings"))
            // ref is lost, and we know type isn't unique; so we are assuming type + property id are unique
            list.add(
                schemaMapOf(
                    "type" to SchemaLiteral(token),
                    "from" to schemaMapOf(
                        "node" to SchemaLiteral(fromRef),
                        "properties" to SchemaMap(fromMappings),
                    ),
                    "to" to schemaMapOf(
                        "node" to SchemaLiteral(toRef),
                        "properties" to SchemaMap(toMappings),
                    ),
                    "table" to tableName,
                    "properties" to SchemaMap(properties),
                )
            )
        }
        return list
    }

    private fun migratePropertyMappings(elements: List<SchemaMap>): MutableMap<String, SchemaElement> {
        val properties = mutableMapOf<String, SchemaElement>()
        for (propertyMapping in elements) {
            val propertyRef = propertyMapping.map("property").string("\$ref").removePrefix("#")
            val fieldName = propertyMapping.literal("fieldName")
            properties[propertyRef] = schemaMapOf(
                "field" to fieldName,
            )
        }
        return properties
    }
}
