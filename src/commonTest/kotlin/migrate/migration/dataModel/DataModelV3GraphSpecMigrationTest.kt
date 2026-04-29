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

import codec.schema.schemaMapOf
import codec.schema.toSchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataModelV3GraphSpecMigrationTest {

    private val migration = DataModelV3GraphSpecMigration()

    @Test
    fun `visualisation throws error if node reference is missing`() {
        val nodes = mutableMapOf("nodeA" to schemaMapOf("id" to "nodeA"))
        val schema = schemaMapOf(
            "visualisation" to schemaMapOf(
                "nodes" to listOf(
                    schemaMapOf(
                        "id" to "nonExistentNode",
                        "position" to mapOf("x" to 10.1, "y" to 20.2)
                    )
                )
            )
        )

        assertFailsWith<IllegalStateException>("Unknown node nonExistentNode") {
            migration.visualisation(schema, nodes)
        }
    }

    @Test
    fun `visualisation transforms to display`() {
        val nodes = mutableMapOf("nodeA" to schemaMapOf("id" to "nodeA"))
        val schema = schemaMapOf(
            "visualisation" to schemaMapOf(
                "nodes" to listOf(
                    schemaMapOf(
                        "id" to "nodeA",
                        "position" to mapOf("x" to 10.1234, "y" to 20.54321)
                    )
                )
            )
        )

        val result = migration.visualisation(schema, nodes)
        assertNotNull(result)
        val node = result.mapOfMaps("nodes")["nodeA"]
        assertNotNull(node)
        assertEquals("10.1234", node.string("x"))
        assertEquals("20.54321", node.string("y"))
    }

    @Test
    fun `gather partitions constraints correctly`() {
        val graphSchema = schemaMapOf(
            "constraints" to listOf(
                schemaMapOf(
                    "entityType" to "node",
                    "nodeLabel" to mapOf("\$ref" to "#L1"),
                    "constraintType" to "uniqueness"
                ),
                schemaMapOf(
                    "entityType" to "relationship",
                    "relationshipType" to mapOf("\$ref" to "#R1"),
                    "constraintType" to "propertyExistence"
                )
            )
        )

        val (nodeCons, relCons) = migration.gather(graphSchema, "constraints")

        assertTrue(nodeCons.containsKey("L1"))
        assertTrue(relCons.containsKey("R1"))
        assertEquals("uniqueness", nodeCons["L1"]?.first()?.string("constraintType"))
    }

    @Test
    fun `migrateNodes handles multiple labels with identifier and implied`() {
        val nodeLabels = listOf(
            schemaMapOf("\$id" to "L1", "token" to "Actor", "properties" to emptyList<Any>()),
            schemaMapOf("\$id" to "L2", "token" to "Person", "properties" to emptyList<Any>()),
            schemaMapOf("\$id" to "L3", "token" to "Human", "properties" to emptyList<Any>())
        )
        val graphSchema = schemaMapOf(
            "nodeLabels" to nodeLabels,
            "nodeObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "nodeObj",
                    "labels" to listOf(mapOf("\$ref" to "#L1"), mapOf("\$ref" to "#L2"), mapOf("\$ref" to "#L3"))
                )
            )
        )

        val nodes = migration.migrateNodes(graphSchema, emptyMap(), emptyMap(), emptyList())

        val node = nodes["nodeObj"]
        assertNotNull(node)
        val labels = node.map("labels")
        assertEquals("Actor", labels.string("identifier"))
        assertEquals(listOf("Person", "Human").toSchemaElement(), labels.list("implied"))
    }

    @Test
    fun `migrateNodes transforms labels and properties`() {
        val labelId = "lbl1"
        val nodeLabels = schemaMapOf(
            labelId to schemaMapOf(
                "\$id" to labelId,
                "token" to "Person",
                "properties" to listOf(
                    schemaMapOf(
                        "\$id" to "prop1",
                        "token" to "name",
                        "type" to mapOf("type" to "string", "nullable" to false)
                    )
                )
            )
        )

        val graphSchema = schemaMapOf(
            "nodeLabels" to listOf(nodeLabels[labelId]),
            "nodeObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "nodeObj1",
                    "labels" to listOf(mapOf("\$ref" to "#$labelId"))
                )
            )
        )

        val nodes = migration.migrateNodes(graphSchema, emptyMap(), emptyMap(), emptyList())

        val migratedNode = nodes["nodeObj1"]
        assertNotNull(migratedNode)
        val labels = migratedNode.map("labels")

        assertEquals("Person", labels.stringOrNull("identifier"))
        val props = migratedNode.map("properties")
        assertNotNull(props["prop1"])
        assertEquals("STRING", props.map("prop1").string("type"))
    }

    @Test
    fun `convertIndexes resolves types and handles names`() {
        val indexes = mapOf(
            "label1" to listOf(
                schemaMapOf(
                    "indexType" to "range",
                    "name" to "custom_idx",
                    "properties" to listOf(mapOf("\$ref" to "#p1"))
                ),
                schemaMapOf(
                    "indexType" to "text",
                    "properties" to listOf(mapOf("\$ref" to "#p2"))
                )
            )
        )

        val result = migration.convertIndexes(indexes, "label1", "Person")
        assertNotNull(result)

        // First index uses custom name
        val idx1 = result["custom_idx"]
        assertNotNull(idx1)
        assertEquals("RANGE", idx1.string("type"))
        assertEquals(listOf("Person").toSchemaElement(), idx1.list("labels"))

        // Second index gets generated name "index2"
        val idx2 = result["index2"]
        assertNotNull(idx2)
        assertEquals("TEXT", idx2.string("type"))
        assertEquals(listOf("p2").toSchemaElement(), idx2.list("properties"))
    }

    @Test
    fun `indexType throws error on unknown index type`() {
        val index = schemaMapOf(
            "indexType" to "UNKNOWN_TYPE",
            "name" to "my_idx",
            "path" to "some.path"
        )

        assertFailsWith<IllegalStateException> {
            migration.convertIndexes(mapOf("L1" to listOf(index)), "L1", "Label")
        }
    }

    @Test
    fun `convertConstraints throws error for multi-property TYPE constraints`() {
        val constraints = mapOf(
            "L1" to listOf(
                schemaMapOf(
                    "constraintType" to "TYPE",
                    "name" to "bad_constraint",
                    "properties" to listOf(mapOf("\$ref" to "#p1"), mapOf("\$ref" to "#p2"))
                )
            )
        )

        assertFailsWith<IllegalStateException>("Type constraints not supported on multiple properties") {
            migration.convertConstraints(constraints, "L1", "Person")
        }
    }

    @Test
    fun `convertConstraints handles property refs and unique names`() {
        val constraints = mapOf(
            "label1" to listOf(
                schemaMapOf(
                    "constraintType" to "uniqueness",
                    "properties" to listOf(mapOf("\$ref" to "#p1"), mapOf("\$ref" to "#p2"))
                )
            )
        )

        val result = migration.convertConstraints(constraints, "label1", "Person")

        assertNotNull(result)
        val constraint = result["constraint1"]
        assertNotNull(constraint)
        assertEquals("UNIQUE", constraint.string("type"))
        assertEquals("Person", constraint.string("label"))
        assertEquals(listOf("p1", "p2").toSchemaElement(), constraint.list("properties"))
    }

    @Test
    fun `check associateByUniqueName handles collisions`() {
        val items = listOf(
            schemaMapOf("name" to "existing"),
            schemaMapOf("name" to "existing"), // Collision
            schemaMapOf() // No name
        )

        val result = with(migration) {
            items.associateByUniqueName("prefix") { schemaMapOf("key" to "value") }
        }

        assertTrue(result.containsKey("existing"))
        assertTrue(result.containsKey("prefix2"))
        assertTrue(result.containsKey("prefix3"))
    }

    @Test
    fun `migrateRelationships handles full transformation`() {
        val relTypeRef = "relType1"
        val graphSchema = schemaMapOf(
            "relationshipTypes" to listOf(
                schemaMapOf(
                    "\$id" to relTypeRef,
                    "token" to "FOLLOWS",
                    "properties" to listOf(
                        schemaMapOf(
                            "\$id" to "p1",
                            "token" to "since",
                            "type" to mapOf("type" to "datetime")
                        )
                    )
                )
            ),
            "relationshipObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "relObj1",
                    "type" to mapOf("\$ref" to "#$relTypeRef"),
                    "from" to mapOf("\$ref" to "#nodeA"),
                    "to" to mapOf("\$ref" to "#nodeB")
                )
            )
        )

        val constraints = mapOf(
            relTypeRef to listOf(
                schemaMapOf(
                    "name" to "c1",
                    "constraintType" to "propertyExistence",
                    "properties" to listOf(mapOf("\$ref" to "#p1"))
                )
            )
        )

        val result = migration.migrateRelationships(graphSchema, constraints, emptyMap())

        assertTrue(result.containsKey("relObj1"))
        val rel = result["relObj1"]
        assertNotNull(rel)
        assertEquals("FOLLOWS", rel.string("type"))
        assertEquals("nodeA", rel.map("from").string("node"))
        assertEquals("ZONED DATETIME", rel.map("properties").map("p1").string("type"))
        assertNotNull(rel.map("constraints")["c1"])
    }

    @Test
    fun `convertProperties normalizes types to uppercase`() {
        val labels = listOf(
            schemaMapOf(
                "properties" to listOf(
                    schemaMapOf(
                        "\$id" to "p1",
                        "token" to "name",
                        "type" to mapOf("type" to "string")
                    ),
                    schemaMapOf(
                        "\$id" to "p2",
                        "token" to "age",
                        "type" to mapOf("type" to "Integer")
                    )
                )
            )
        )

        val result = migration.convertProperties(labels, emptySet())

        assertEquals("STRING", result["p1"]?.string("type"))
        assertEquals("INTEGER", result["p2"]?.string("type"))
    }

    @Test
    fun `relationshipMappings joins object types and tokens correctly`() {
        val input = schemaMapOf(
            "graphSchemaRepresentation" to mapOf(
                "graphSchema" to mapOf(
                    "relationshipObjectTypes" to listOf(
                        schemaMapOf(
                            "\$id" to "obj1",
                            "type" to mapOf("\$ref" to "#type1"),
                            "from" to mapOf("\$ref" to "#A"),
                            "to" to mapOf("\$ref" to "#B")
                        )
                    ),
                    "relationshipTypes" to listOf(
                        schemaMapOf("\$id" to "type1", "token" to "WORKS_AT")
                    )
                )
            ),
            "graphMappingRepresentation" to mapOf(
                "relationshipMappings" to listOf(
                    schemaMapOf(
                        "relationship" to mapOf("\$ref" to "#obj1"),
                        "tableName" to "WORKS_IN",
                        "fromMappings" to mapOf("#propA" to "COL_A"), // Tests prefix removal
                        "toMappings" to mapOf("#propB" to "COL_B"),
                        "propertyMappings" to emptyList<Any>()
                    )
                )
            )
        )

        val result = migration.relationshipMappings(unwrap(input))

        val mapping = result.first()
        assertEquals("WORKS_AT", mapping.string("relationship"))
        assertEquals("WORKS_IN", mapping.string("table"))

        val fromProps = mapping.map("from").map("properties")
        assertTrue(fromProps.containsKey("propA"))
        assertEquals("COL_A", fromProps.map("propA").string("field"))
    }

    @Test
    fun `relationshipMappings throws error when relationship object type is missing`() {
        val schema = schemaMapOf(
            "graphSchemaRepresentation" to mapOf(
                "graphSchema" to mapOf(
                    "relationshipObjectTypes" to emptyList<Any>(),
                    "relationshipTypes" to emptyList()
                )
            ),
            "graphMappingRepresentation" to mapOf(
                "relationshipMappings" to listOf(
                    schemaMapOf("relationship" to mapOf("\$ref" to "#missingRel"))
                )
            )
        )

        assertFailsWith<IllegalStateException>("Relationship missingRel not found") {
            migration.relationshipMappings(unwrap(schema))
        }
    }

    @Test
    fun `nodeMappings transforms correctly`() {
        val input = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "nodeMappings" to listOf(
                    schemaMapOf(
                        "node" to mapOf("\$ref" to "#node1"),
                        "tableName" to "USERS_TABLE",
                        "propertyMappings" to listOf(
                            schemaMapOf(
                                "property" to mapOf("\$ref" to "#prop1"),
                                "fieldName" to "USER_NAME"
                            )
                        )
                    )
                )
            )
        )

        val result = migration.nodeMappings(unwrap(input))

        assertEquals(1, result.size)
        val mapping = result.first()
        assertEquals("node1", mapping.string("node"))
        assertEquals("USERS_TABLE", mapping.string("table"))
        assertEquals("USER_NAME", mapping.map("properties").map("prop1").string("field"))
    }

    @Test
    fun `migrateTables handles foreign keys`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "type" to "JDBC",
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "users",
                            "fields" to listOf(
                                schemaMapOf("name" to "id", "rawType" to "INT")
                            ),
                            "foreignKeys" to listOf(
                                schemaMapOf(
                                    "referencedTable" to "accounts",
                                    "fields" to listOf(
                                        mapOf("field" to "account_id", "referencedField" to "id")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val tables = migration.migrateTables(unwrap(inputSchema))

        val userTable = tables["users"]
        assertNotNull(userTable)
        val fks = userTable.mapOfMaps("foreignKeys")
        assertEquals(1, fks.size)
        val firstFK = fks.values.first()
        assertEquals("accounts", firstFK.map("references").string("table"))
    }

    @Test
    fun `migrateTables excludes empty foreignKeys and primaryKeys`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "empty_table",
                            "fields" to listOf(mapOf("name" to "id")),
                            "primaryKeys" to emptyList<String>(),
                            "foreignKeys" to emptyList<Any>()
                        )
                    )
                )
            )
        )

        val tables = migration.migrateTables(unwrap(inputSchema))
        val table = tables["empty_table"]!!

        assertFalse(table.containsKey("primaryKeys"), "primaryKeys should be omitted if empty")
        assertFalse(table.containsKey("foreignKeys"), "foreignKeys should be omitted if empty")
    }
}
