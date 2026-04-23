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
package codec.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaElementTest {

    @Test
    fun `repath updates entire tree recursively`() {
        val root = SchemaMap(
            mutableMapOf(
                "users" to SchemaList(
                    mutableListOf(
                        SchemaMap(mutableMapOf("name" to SchemaLiteral("Alice")))
                    )
                )
            )
        )

        val updated = root.repath("root")

        assertEquals("root", updated.path)
        assertEquals("root.users", updated.list("users").path)
        assertEquals("root.users[0]", updated.list("users")[0].path)
        assertEquals("root.users[0].name", (updated.list("users")[0] as SchemaMap).literal("name").path)
    }

    @Test
    fun `SchemaMap set and put maintain correct paths`() {
        val root = SchemaMap(path = "root")

        // Using operator set
        root["age"] = "30"
        assertEquals("root.age", root.literal("age").path)

        // Using put
        root.put("name", SchemaLiteral("Bob"))
        assertEquals("root.name", root.literal("name").path)
    }

    @Test
    fun `SchemaNull always has a path`() {
        val repathed = SchemaNull("any.path")
        assertEquals("any.path", repathed.path)
    }

    private val data = SchemaMap().apply {
        this["str"] = "hello"
        this["num"] = "123"
        this["flag"] = "true"
        this["empty"] = SchemaNull()
        this["nested_map"] = mapOf("key" to SchemaLiteral("val"))
        this["nested_list"] = listOf(SchemaLiteral("item"))
    }

    @Test
    fun `string accessors`() {
        assertEquals("hello", data.string("str"))
        assertEquals("hello", data.stringOrNull("str"))
        assertNull(data.stringOrNull("missing"))
    }

    @Test
    fun `int accessors and coercion`() {
        assertEquals(123, data.int("num"))
        assertEquals(123, data.intOrNull("num"))
        assertNull(data.intOrNull("str")) // Non-numeric string
    }

    @Test
    fun `boolean accessors and coercion`() {
        assertEquals(true, data.bool("flag"))
        assertEquals(true, data.boolOrNull("flag"))
        assertFailsWith<IllegalArgumentException> { data.bool("str") }
    }

    @Test
    fun `map and list helpers`() {
        assertIs<SchemaMap>(data.map("nested_map"))
        assertIs<SchemaList>(data.list("nested_list"))

        assertFailsWith<IllegalStateException> { data.map("str") } // Type mismatch
        assertFailsWith<IllegalStateException> { data.list("missing") } // Missing key
    }

    @Test
    fun `mapOrPut creates map if missing`() {
        val root = SchemaMap(path = "root")
        val autoMap = root.mapOrPut("new_section")

        assertEquals("root.new_section", autoMap.path)
        assertTrue(root.containsKey("new_section"))
    }

    @Test
    fun `mapOfMaps transformation`() {
        val root = SchemaMap()
        val inner = SchemaMap()
        inner["id"] = "1"
        root.put("registry", SchemaMap(mutableMapOf("node_a" to inner)))

        val result = root.mapOfMaps("registry")
        assertEquals("1", result["node_a"]?.string("id"))
    }

    @Test
    fun `listOfMaps transformation`() {
        val root = SchemaMap()
        val item1 = SchemaMap().apply { this["val"] = "A" }
        val item2 = SchemaMap().apply { this["val"] = "B" }
        root["items"] = listOf(item1, item2)

        val result = root.listOfMaps("items")
        assertEquals(2, result.size)
        assertEquals("A", result[0].string("val"))
    }

    @Test
    fun `equality is based on content not path`() {
        val literal1 = SchemaLiteral("value", "path.a")
        val literal2 = SchemaLiteral("value", "path.b")

        assertEquals(literal1, literal2)
        assertEquals(literal1.hashCode(), literal2.hashCode())
    }

    @Test
    fun `toString produces valid looking structures`() {
        val map = SchemaMap()
        map["id"] = "1"
        map["tags"] = listOf(SchemaLiteral("a"))

        val output = map.toString()
        // {"id":1,"tags":[a]}
        assertTrue(output.contains("\"id\":1"))
        assertTrue(output.contains("\"tags\":[a]"))
    }

    @Test
    fun `putAll repaths all elements`() {
        val root = SchemaMap(path = "root")
        val externalData = mapOf(
            "ext" to SchemaLiteral("value", "wrong.path")
        )

        root.putAll(externalData)
        assertEquals("root.ext", root.literal("ext").path)
    }

    @Test
    fun `removeMap throws if element is not a map`() {
        val root = SchemaMap()
        root["key"] = "literal"

        assertFailsWith<IllegalStateException> {
            root.removeMap("key")
        }
    }
}
