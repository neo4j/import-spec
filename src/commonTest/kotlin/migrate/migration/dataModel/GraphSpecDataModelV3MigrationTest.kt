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
import codec.schema.schemaMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
}
