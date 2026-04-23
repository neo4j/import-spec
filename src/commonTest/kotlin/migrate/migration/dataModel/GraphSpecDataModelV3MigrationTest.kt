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
import codec.schema.SchemaNull
import codec.schema.schemaMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphSpecDataModelV3MigrationTest {

    private val migration = GraphSpecDataModelV3Migration()

    @Test
    fun `migrate converts node labels and properties correctly`() {
        val input = schemaMapOf(
            "version" to "2.0",
            "nodes" to schemaMapOf(
                "node1" to schemaMapOf(
                    "labels" to schemaMapOf(
                        "identifier" to "Person",
                        "implied" to listOf("Entity")
                    ),
                    "properties" to schemaMapOf(
                        "p1" to schemaMapOf("name" to "name", "type" to "STRING", "nullable" to true)
                    )
                )
            )
        )

        val result = migration.migrate(input)

        // Verify Node Labels
        val nodeLabels = result.map(
            "dataModel"
        ).map("graphSchemaRepresentation").map("graphSchema").listOfMaps("nodeLabels")
        assertEquals(2, nodeLabels.size)
        val personLabel = nodeLabels.first { it.string("token") == "Person" }
        assertEquals("nl:0", personLabel.string("\$id"))

        // Verify Properties moved to Label
        val props = personLabel.listOfMaps("properties")
        assertEquals("p1", props[0].string("\$id"))
        assertEquals("string", props[0].map("type").string("type"))

        // Verify Object Type
        val nodeObjectTypes = result.map(
            "dataModel"
        ).map("graphSchemaRepresentation").map("graphSchema").listOfMaps("nodeObjectTypes")
        assertEquals("node1", nodeObjectTypes[0].string("\$id"))
        val labelRefs = nodeObjectTypes[0].listOfMaps("labels")
        assertEquals("#nl:0", labelRefs[0].string("\$ref"))
    }

    @Test
    fun `migrate deduplicates properties when multiple nodes share the same primary label`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING"))
                ),
                "n2" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING"))
                )
            )
        )

        val result = migration.migrate(input)
        val nodeLabels = result.map("dataModel").map("graphSchemaRepresentation")
            .map("graphSchema").listOfMaps("nodeLabels")

        // The logic creates a new ID nl:0, nl:1 for every label found in the loop.
        // However, the test verifies if the properties list for a specific label ID remains unique.
        val personLabel = nodeLabels.first { it.string("token") == "Person" }
        val props = personLabel.listOfMaps("properties")
        assertEquals(1, props.size, "Property p1 should not be duplicated in the label definition")
    }

    @Test
    fun `migrate converts relationships and handles ObjectType references`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf("n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "A"))),
            "relationships" to schemaMapOf(
                "rel1" to schemaMapOf(
                    "type" to "WORKS_AT",
                    "from" to schemaMapOf("node" to "n1"),
                    "to" to schemaMapOf("node" to "n2"),
                    "properties" to schemaMapOf(
                        "since" to schemaMapOf("name" to "since", "type" to "INTEGER")
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val schema = result.map("dataModel").map("graphSchemaRepresentation").map("graphSchema")

        // Verify Relationship Type
        val relTypes = schema.listOfMaps("relationshipTypes")
        assertEquals("rt:rel1", relTypes[0].string("\$id"))

        // Verify Relationship Object Type (the link between nodes)
        val relObjectTypes = schema.listOfMaps("relationshipObjectTypes")
        assertEquals("rel1", relObjectTypes[0].string("\$id"))
        assertEquals("#rt:rel1", relObjectTypes[0].map("type").string("\$ref"))
        assertEquals("#n1", relObjectTypes[0].map("from").string("\$ref"))
    }

    @Test
    fun `migrate handles relationship constraints and sets relationshipType ref`() {
        val input = schemaMapOf(
            "relationships" to schemaMapOf(
                "r:1" to schemaMapOf(
                    "type" to "LIVES_IN",
                    "from" to schemaMapOf("node" to "n1"),
                    "to" to schemaMapOf("node" to "n2"),
                    "constraints" to schemaMapOf(
                        "rel_uniq" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("p1"))
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val constraints = result.map("dataModel").map("graphSchemaRepresentation")
            .map("graphSchema").listOfMaps("constraints")

        val relConstraint = constraints[0]
        assertEquals("c:1", relConstraint.string("\$id"))
        assertEquals("relationship", relConstraint.string("entityType"))
        assertEquals("#rt:1", relConstraint.map("relationshipType").string("\$ref"))
        // Ensure nodeLabel is specifically SchemaNull
        assertTrue(relConstraint["nodeLabel"] is SchemaNull)
    }

    @Test
    fun `migrate handles constraints and indexes with type transformation`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "User"),
                    "constraints" to schemaMapOf(
                        "uniq_id" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("p1"))
                    ),
                    "indexes" to schemaMapOf(
                        "idx_name" to schemaMapOf("type" to "RANGE", "properties" to listOf("p2"))
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val graphSchema = result.map("dataModel").map("graphSchemaRepresentation").map("graphSchema")

        val constraint = graphSchema.listOfMaps("constraints")[0]
        assertEquals("uniqueness", constraint.string("constraintType"))
        assertEquals("#nl:0", constraint.map("nodeLabel").string("\$ref"))

        val index = graphSchema.listOfMaps("indexes")[0]
        assertEquals("range", index.string("indexType"))
    }

    @Test
    fun `convertGraphMapping correctly recovers relationship IDs via findRelationshipId`() {
        val input = schemaMapOf(
            "relationships" to schemaMapOf(
                "actual_rel_id" to schemaMapOf(
                    "type" to "FOLLOWS",
                    "from" to schemaMapOf("node" to "User"),
                    "to" to schemaMapOf("node" to "User")
                )
            ),
            "mappings" to listOf(
                schemaMapOf(
                    "relationship" to "FOLLOWS",
                    "table" to "user_follows",
                    "from" to
                        schemaMapOf(
                            "node" to "User",
                            "properties" to schemaMapOf("uid" to schemaMapOf("field" to "from_id"))
                        ),
                    "to" to
                        schemaMapOf(
                            "node" to "User",
                            "properties" to schemaMapOf("uid" to schemaMapOf("field" to "to_id"))
                        )
                )
            )
        )

        val result = migration.migrate(input)
        val relMappings = result.map("dataModel").map("graphMappingRepresentation").listOfMaps("relationshipMappings")

        assertEquals(1, relMappings.size)
        // Verify that findRelationshipId matched "FOLLOWS" + "User" -> "User" to "actual_rel_id"
        assertEquals("#actual_rel_id", relMappings[0].map("relationship").string("\$ref"))
        assertEquals("user_follows", relMappings[0].string("tableName"))
    }

    @Test
    fun `convertGraphMapping ignores relationship mappings that cannot be resolved`() {
        val input = schemaMapOf(
            "relationships" to schemaMapOf(), // Empty
            "mappings" to listOf(
                schemaMapOf(
                    "relationship" to "NON_EXISTENT",
                    "from" to schemaMapOf("node" to "A"),
                    "to" to schemaMapOf("node" to "B")
                )
            )
        )

        val result = migration.migrate(input)
        val relMappings = result.map(
            "dataModel"
        ).map("graphMappingRepresentation").listOfMapsOrNull("relationshipMappings")
        assertNull(relMappings)
    }

    @Test
    fun `convertExtensions identifies key properties only when non-nullable and unique`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "properties" to schemaMapOf(
                        "p1" to schemaMapOf("nullable" to false, "unique" to true), // Key
                        "p2" to schemaMapOf("nullable" to true, "unique" to true), // Not Key (nullable)
                        "p3" to schemaMapOf("nullable" to false, "unique" to false) // Not Key (not unique)
                    )
                )
            )
        )

        val result = migration.convertExtensions(input)
        assertNotNull(result)
        val nodeKeyProps = result.listOfMaps("nodeKeyProperties")

        assertEquals(1, nodeKeyProps.size)
        assertEquals("#n1", nodeKeyProps[0].map("node").string("\$ref"))
        val keys = nodeKeyProps[0].listOfMaps("keyProperties")
        assertEquals(1, keys.size)
        assertEquals("#p1", keys[0].string("\$ref"))
    }

    @Test
    fun `convertSourceSchema handles complex foreign keys correctly`() {
        val input = schemaMapOf(
            "tables" to schemaMapOf(
                "Orders" to schemaMapOf(
                    "source" to "SQL",
                    "foreignKeys" to schemaMapOf(
                        "fk_customer" to schemaMapOf(
                            "fields" to listOf("cust_id", "region_id"),
                            "references" to schemaMapOf(
                                "table" to "Customers",
                                "fields" to listOf("id", "reg_id")
                            )
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val tableSchemas = result.map("dataModel").map("graphMappingRepresentation")
            .map("dataSourceSchema").listOfMaps("tableSchemas")

        val fk = tableSchemas[0].listOfMaps("foreignKeys")[0]
        assertEquals("Customers", fk.string("referencedTable"))

        val fields = fk.listOfMaps("fields")
        assertEquals(2, fields.size)
        assertEquals("cust_id", fields[0].string("field"))
        assertEquals("id", fields[0].string("referencedField"))
        assertEquals("region_id", fields[1].string("field"))
        assertEquals("reg_id", fields[1].string("referencedField"))
    }

    @Test
    fun `convertFields transforms raw types to camelCase recommended types`() {
        val fieldsInput = mapOf(
            "f1" to schemaMapOf(
                "name" to "first_name",
                "suggested" to "STRING_TYPE",
                "supported" to listOf(SchemaLiteral("STRING_TYPE"), SchemaLiteral("LONG_TEXT"))
            )
        )

        val fields = migration.convertFields(fieldsInput)

        assertEquals("first_name", fields[0].string("name"))
        assertEquals("stringType", fields[0].map("recommendedType").string("type"))
        val supported = fields[0].listOfMaps("supportedTypes")
        assertEquals("stringType", supported[0].string("type"))
        assertEquals("longText", supported[1].string("type"))
    }

    @Test
    fun `convertVisualisation transforms coordinates correctly`() {
        val display = schemaMapOf(
            "display" to schemaMapOf(
                "nodes" to schemaMapOf(
                    "node1" to schemaMapOf("x" to 100.23, "y" to 200.12)
                )
            )
        )

        val result = migration.convertVisualisation(display)

        assertNotNull(result)
        val nodes = result.listOfMaps("nodes")
        assertEquals("node1", nodes[0].string("id"))
        assertEquals(100.23, nodes[0].map("position").string("x").toDouble())
        assertEquals(200.12, nodes[0].map("position").string("y").toDouble())
    }

    @Test
    fun `migrate handles completely empty schema without crashing`() {
        val emptyInput = schemaMapOf("version" to "2.0")

        val result = migration.migrate(emptyInput)

        val graphSchema = result.map("dataModel").map("graphSchemaRepresentation").map("graphSchema")

        // toNotEmpty should cause these to be missing or empty depending on implementation
        assertFalse(graphSchema.containsKey("nodeLabels"))
        assertFalse(graphSchema.containsKey("relationshipTypes"))
        assertEquals(0, graphSchema.listOfMaps("constraints").size)
        assertEquals(0, graphSchema.listOfMaps("indexes").size)
    }
}
