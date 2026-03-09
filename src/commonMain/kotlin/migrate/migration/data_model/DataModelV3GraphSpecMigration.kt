package migrate.migration.data_model

import codec.schema.SchemaElement
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.schemaListOf
import codec.schema.schemaMapOf
import migrate.Migration
import model.Version

class DataModelV3GraphSpecMigration : Migration(Version.DATA_MODEL_V30, Version.LATEST) {
    // TODO should migration rely on exceptions?

    override fun migrate(schema: SchemaMap): SchemaMap {
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val nodes = migrateNodes(graphSchema)
        val relationships = migrateRelationships(graphSchema)
        return schemaMapOf(
            "nodes" to SchemaMap(nodes),
            "relationships" to SchemaMap(relationships),
        )
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
        for (objectType in schema.list("relationshipObjectTypes")) {
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
}
