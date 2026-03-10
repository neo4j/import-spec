package migrate.migration.data_model

import codec.schema.SchemaMap
import codec.schema.schemaListOf
import codec.schema.schemaMapOf
import migrate.Migration
import model.Version

/**
 * 2.0 -> 3.0 adds support for multiple keys properties
 */
class DataModelV2V3Migration(version: String) : Migration(version, Version.DATA_MODEL_V30) {
    override fun migrate(schema: SchemaMap): SchemaMap {
        // Replace singular keyProperty with a list keyProperties
        val extensions = schema.map("graphSchemaExtensionsRepresentation")
        val nodeKeyProperties = extensions.listOfMaps("nodeKeyProperties")
        val references = mutableMapOf<String, String>()
        for (property in nodeKeyProperties) {
            val key = property.map("keyProperty")
            property.remove("keyProperty")
            property["keyProperties"] = schemaListOf(key)
            // Store refs for node -> property
            val nodeRef = property.map("node").string("\$ref")
            val keyRef = key.string("\$ref")
            references[nodeRef] = keyRef
        }

        // Replace from/toMapping with from/toMappings
        val mappings = schema.map("graphMappingRepresentation")
        val relationshipMappings = mappings.listOfMaps("relationshipMappings")
        val relationshipObjectTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("relationshipObjectTypes")
        for (mapping in relationshipMappings) {
            // Find relationship object and it's node refs
            val relationshipRef = mapping.map("relationship").string("\$ref").removePrefix("#")
            val objectType = relationshipObjectTypes
                .firstOrNull { it.string("\$id") == relationshipRef }
                ?: error("Could not find relationship object $relationshipRef")
            val fromNodeRef = objectType.map("from").string("\$ref")
            val toNodeRef = objectType.map("to").string("\$ref")

            // Resolve nodeKeyProperties and convert mapping into mappings
            updateMapping(references, mapping, fromNodeRef, "fromMapping")
            updateMapping(references, mapping, toNodeRef, "toMapping")
        }
        return schema
    }

    private fun updateMapping(
        references: MutableMap<String, String>,
        mapping: SchemaMap,
        nodeRef: String,
        key: String
    ) {
        val toPropertyRef =
            references[nodeRef] ?: error("Unable to resolve nodeKeyProperties to node ref $nodeRef")
        val toFieldName = mapping.map(key).literal("fieldName")
        mapping["${key}s"] = schemaListOf(schemaMapOf(toPropertyRef to toFieldName))
        mapping.remove(key)
    }
}
