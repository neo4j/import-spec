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

import codec.schema.SchemaMap
import codec.schema.schemaMapOf
import migrate.Migration
import model.Type
import model.Version

/**
 * 2.X -> 3.0 adds support for multiple keys properties
 */
class DataModelV2V3Migration(version: String) :
    Migration(
        fromType = Type.DATA_MODEL,
        from = version,
        toType = Type.DATA_MODEL,
        to = Version.DATA_MODEL_V30
    ) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val schema = unwrap(schema)
        // Replace singular keyProperty with a list keyProperties
        val nodeKeyProperties = schema
            .map("graphSchemaExtensionsRepresentation")
            .listOfMaps("nodeKeyProperties")
        val references = mutableMapOf<String, String>()
        for (property in nodeKeyProperties) {
            val key = property.removeMap("keyProperty")
            property["keyProperties"] = mutableListOf(key)
            // Store refs for node -> property
            val nodeRef = property.ref("node")
            references[nodeRef] = key.ref()
        }

        // Replace from/toMapping with from/toMappings
        val relationshipObjectTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("relationshipObjectTypes")
            .associateBy { it.id() }
        for (mapping in schema
            .map("graphMappingRepresentation")
            .listOfMaps("relationshipMappings")) {
            // Find relationship object and it's node refs
            val relationshipRef = mapping.ref("relationship")
            val objectType = relationshipObjectTypes[relationshipRef]
                ?: error("Could not find relationship object $relationshipRef")
            val fromNodeRef = objectType.ref("from")
            val toNodeRef = objectType.ref("to")

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
        mapping["${key}s"] = schemaMapOf(toPropertyRef to toFieldName)
        mapping.remove(key)
    }
}
