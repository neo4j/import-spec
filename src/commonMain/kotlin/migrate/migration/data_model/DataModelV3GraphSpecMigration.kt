package migrate.migration.data_model

import codec.schema.SchemaElement
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.schemaListOf
import codec.schema.schemaMapOf
import migrate.Migration
import model.Version
import model.source.TableField

class DataModelV3GraphSpecMigration : Migration(Version.DATA_MODEL_V30, Version.LATEST) {
    // TODO should migration rely on exceptions?

    override fun migrate(schema: SchemaMap): SchemaMap {
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val nodes = migrateNodes(graphSchema)
        val relationships = migrateRelationships(graphSchema)
        val mappings = migrateMappings(schema)
        val tables = migrateTables(schema)
        return schemaMapOf(
            "nodes" to SchemaMap(nodes),
            "relationships" to SchemaMap(relationships),
            "tables" to SchemaMap(tables),
            "mappings" to SchemaList(mappings),
        )
    }

    private fun migrateTables(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val tables = mutableMapOf<String, SchemaElement>()
        val sourceSchema = schema.map("graphMappingRepresentation").map("dataSourceSchema")
        val sourceType = sourceSchema.string("type")
        for (table in sourceSchema.list("tableSchemas")) {
            if (table !is SchemaMap) {
                error("TableSchema must be a map")
            }

            val fields = mutableMapOf<String, SchemaElement>()
            val name = table.string("name")
            for (field in table.list("fields")) {
                if (field !is SchemaMap) {
                    error("Field must be a map")
                }
                val name = field.literal("name")
                val rawType = field.literal("rawType")
                val size = field.literal("size")
                val recommendedObject = field.map("recommendedType")
                val recommendedType = recommendedObject.string("type")
                val supportedTypes = field.list("supportedTypes").map { (it as SchemaMap).string("type").uppercase() } // FIXME array types
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
            for (foreignKey in table.list("foreignKeys")) {
                if (foreignKey !is SchemaMap) {
                    error("ForeignKey must be a map")
                }
                val tableRef = foreignKey.literal("referencedTable")
                val fields = mutableListOf<SchemaElement>()
                val referencedFields = mutableListOf<SchemaElement>()
                for (field in foreignKey.list("fields")) {
                    if (field !is SchemaMap) {
                        error("Field must be a map")
                    }
                    fields.add(field.literal("field"))
                    referencedFields.add(field.literal("referencedField"))
                }
                foreignKeys["foreign_key${foreignKeys.size + 1}"] = schemaMapOf(
                    "fields" to SchemaList(fields),
                    "references" to schemaMapOf(
                        "table" to tableRef,
                        "fields" to SchemaList(referencedFields),
                    )
                )
            }
            tables[name] = schemaMapOf(
                "source" to SchemaLiteral(sourceType),
                "fields" to SchemaMap(fields),
                "primaryKeys" to primaryKeys,
                "foreignKeys" to SchemaMap(foreignKeys),
            )
        }
        return tables
    }

    private fun migrateNodes(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val nodes = mutableMapOf<String, SchemaElement>()
        val nodeLabels = schema.list("nodeLabels")
        for (nodeObject in schema.list("nodeObjectTypes")) {
            if (nodeObject !is SchemaMap) {
                error("Node object types must be a map")
            }
            val nodeRef = nodeObject.string("\$id")
            val labelRefs = nodeObject.list("labels").map { (it as SchemaMap).string("\$ref").removePrefix("#") }

            val labels = labelRefs.map { labelRef ->
                nodeLabels.firstOrNull { it is SchemaMap && it.string("\$id") == labelRef } as? SchemaMap
                    ?: error("Label $labelRef not found")
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
            nodes[nodeRef] = node
        }
        return nodes
    }

    private fun convertProperties(
        label: SchemaMap,
        properties: MutableMap<String, SchemaElement>
    ) {
        for (property in label.list("properties")) {
            if (property !is SchemaMap) {
                error("Node label property must be a map")
            }
            val ref = property.string("\$id")
            val token = property.literal("token")
            val typeObj = property.map("type") // TODO arrays
            val type = typeObj.string("type").uppercase()
            // TODO constraints
            val map = schemaMapOf(
                "name" to token,
                "type" to SchemaLiteral(type),
            )
            val nullable = typeObj.literalOrNull("nullable")
            if (nullable != null) {
                map["nullable"] = nullable
            }
            properties[ref] = map
        }
    }

    private fun migrateRelationships(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val relationships = mutableMapOf<String, SchemaElement>()
        for (objectType in schema
            .list("relationshipObjectTypes")) {
            if (objectType !is SchemaMap) {
                error("Relationship object type must be a map")
            }
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").literal("\$ref")
            val toRef = objectType.map("to").literal("\$ref")


            val relationshipType = schema.list("relationshipTypes")
                .firstOrNull { it is SchemaMap && it.string("\$id") == typeRef } as? SchemaMap
                ?: error("RelationshipType $typeRef not found")
            val token = relationshipType.literal("token")
            val properties = mutableMapOf<String, SchemaElement>()
            convertProperties(relationshipType, properties)
            val relationship = schemaMapOf(
                "type" to token,
                "from" to fromRef,
                "to" to toRef,
                "properties" to SchemaMap(properties)
            )
            relationships[ref] = relationship
        }
        return relationships
    }

    private fun migrateMappings(schema: SchemaMap): MutableList<SchemaElement> {
        val list = mutableListOf<SchemaElement>()
        for (nodeMapping in schema
            .map("graphMappingRepresentation")
            .list("nodeMappings")) {
            if (nodeMapping !is SchemaMap) {
                error("Node mapping type must be a map")
            }
            val ref = nodeMapping.map("node").literal("\$ref")
            val tableName = nodeMapping.literal("tableName")

            val properties = migratePropertyMappings(nodeMapping.list("propertyMappings"))
            list.add(
                schemaMapOf(
                    "node" to ref,
                    "table" to tableName,
                    "properties" to SchemaMap(properties),
                )
            )
        }

        val relationships = mutableMapOf<String, Triple<String, String, String>>()
        for (objectType in schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .list("relationshipObjectTypes")) {
            if (objectType !is SchemaMap) {
                error("Relationship object type must be a map")
            }
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").string("\$ref")
            val toRef = objectType.map("to").string("\$ref")
            relationships[ref] = Triple(fromRef, typeRef, toRef)
        }

        for (relationshipMapping in schema
            .map("graphMappingRepresentation")
            .list("relationshipMappings")) {
            if (relationshipMapping !is SchemaMap) {
                error("Relationship mapping type must be a map")
            }
            val ref = relationshipMapping.map("relationship").string("\$ref").removePrefix("#")
            val (fromRef, typeRef, toRef) = relationships[ref] ?: error("Relationship $ref not found")

            val tableName = relationshipMapping.literal("tableName")

            val fromMappings = mutableMapOf<String, SchemaElement>()
            for ((key, value) in relationshipMapping.map("fromMappings")) {
                fromMappings[key] = schemaMapOf("field" to value as SchemaLiteral)
            }

            val toMappings = mutableMapOf<String, SchemaElement>()
            for ((key, value) in relationshipMapping.map("toMappings")) {
                toMappings[key] = schemaMapOf("field" to value as SchemaLiteral)
            }

            val properties = migratePropertyMappings(relationshipMapping.list("propertyMappings"))
            list.add(
                schemaMapOf(
                    "type" to SchemaLiteral(typeRef),
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

    private fun migratePropertyMappings(elements: SchemaList): MutableMap<String, SchemaElement> {
        val properties = mutableMapOf<String, SchemaElement>()
        for (propertyMapping in elements) {
            if (propertyMapping !is SchemaMap) {
                error("Property mapping type must be a map")
            }
            val propertyRef = propertyMapping.map("property").string("\$ref")
            val fieldName = propertyMapping.literal("fieldName")
            properties[propertyRef] = schemaMapOf(
                "field" to fieldName,
            )
        }
        return properties
    }
}
