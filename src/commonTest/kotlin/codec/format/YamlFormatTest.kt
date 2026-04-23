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
package codec.format

import codec.schema.SchemaMap
import codec.schema.SchemaNull
import codec.schema.SchemaLiteral
import model.GraphModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YamlFormatTest {

    private val yamlFormat = YamlFormat.default

    @Test
    fun `test yaml path generation matches json logic`() {
        val yaml = """
            nodes:
              user_node:
                labels:
                  - Person
                  - Admin
        """.trimIndent()

        val schema = yamlFormat.decodeFromString(yaml) as SchemaMap

        // Map path
        val nodes = schema.map("nodes")
        assertEquals("nodes", nodes.path)

        val userNode = nodes.map("user_node")
        assertEquals("nodes.user_node", userNode.path)

        // List path
        val labels = userNode.list("labels")
        assertEquals("nodes.user_node.labels[0]", (labels[0] as SchemaLiteral).path)
        assertEquals("nodes.user_node.labels[1]", (labels[1] as SchemaLiteral).path)
    }

    @Test
    fun `test yaml null variants`() {
        val yaml = """
            key1: null
            key2: ~
        """.trimIndent()

        val schema = yamlFormat.decodeFromString(yaml) as SchemaMap

        assertIs<SchemaNull>(schema.content["key1"])
        assertIs<SchemaNull>(schema.content["key2"])
    }

    @Test
    fun `test round-trip yaml to schema and back`() {
        val originalYaml = """
            version: "1.0"
            active: "true"
            tags:
            - "a"
            - "b"
        """.trimIndent()

        val schema = yamlFormat.decodeFromString(originalYaml)
        val output = yamlFormat.encodeToString(schema)

        val reDecoded = yamlFormat.decodeFromString(output) as SchemaMap
        assertEquals("1.0", reDecoded.string("version"))
        assertEquals(2, reDecoded.list("tags").size)
    }

    @Test
    fun `test delegation to json for schema encoding`() {
        val model = GraphModel(version = "v2")

        val schema = yamlFormat.encodeToSchema(model) as SchemaMap
        assertEquals("v2", schema.string("version"))

        val decoded = yamlFormat.decodeFromSchema(schema)
        assertEquals("v2", decoded.version)
    }

    @Test
    fun `test deep nesting in yaml`() {
        val yaml = """
            a:
              b:
                c:
                  - d: "val"
        """.trimIndent()

        val schema = yamlFormat.decodeFromString(yaml) as SchemaMap
        val valElement = schema.map("a").map("b").list("c")[0] as SchemaMap

        assertEquals("val", valElement.string("d"))
        assertEquals("a.b.c[0].d", valElement.literal("d").path)
    }

    @Test
    fun `test toYaml handles all schema types`() {
        val schemaMap = SchemaMap()
        schemaMap["name"] = "test"
        schemaMap["items"] = listOf(SchemaLiteral("item1"), SchemaNull())

        val yamlString = yamlFormat.encodeToString(schemaMap)

        assertTrue(yamlString.contains("name: test"))
        assertTrue(yamlString.contains("- item1"))
        assertTrue(yamlString.contains("- null") || yamlString.contains("~"))
    }
}
