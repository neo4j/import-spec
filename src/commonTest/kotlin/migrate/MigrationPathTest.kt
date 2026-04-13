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
package migrate

import codec.schema.SchemaMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MigrationPathTest {

    @Test
    fun `findPath - returns empty list when source and target are identical`() {
        val engine = createPath()
        val path = engine.findPath("type:1.0.0", "type:1.0.0")
        assertNotNull(path)
        assertTrue(path.isEmpty())
    }

    @Test
    fun `findPath - finds a direct single step path`() {
        val m1 = TestMigration("1.0.0", "1.1.0")
        val engine = createPath(m1)

        val path = engine.findPath("type:1.0.0", "type:1.1.0")

        assertEquals(1, path?.size)
        assertEquals("1.1.0", path?.first()?.to)
    }

    @Test
    fun `findPath - finds a multi-step path`() {
        val m1 = TestMigration("1.0.0", "1.1.0")
        val m2 = TestMigration("1.1.0", "1.2.0")
        val engine = createPath(m1, m2)

        val path = engine.findPath("type:1.0.0", "type:1.2.0")

        assertEquals(2, path?.size)
        assertEquals("1.1.0", path!![0].to)
        assertEquals("1.2.0", path[1].to)
    }

    @Test
    fun `findPath - finds the shortest path`() {
        // Path 1: 1.0 -> 1.1 -> 1.2 (2 steps)
        // Path 2: 1.0 -> 1.2 (1 step)
        val m1 = TestMigration("1.0.0", "1.1.0")
        val m2 = TestMigration("1.1.0", "1.2.0")
        val m3 = TestMigration("1.0.0", "1.2.0")
        val engine = createPath(m1, m2, m3)

        val path = engine.findPath("type:1.0.0", "type:1.2.0")

        assertEquals(1, path?.size, "Should have chosen the direct shortcut")
        assertEquals("1.2.0", path!![0].to)
    }

    @Test
    fun `findPath - returns null when no path exists`() {
        val m1 = TestMigration("1.0.0", "1.1.0")
        val engine = createPath(m1)

        val path = engine.findPath("type:1.0.0", "type:2.0.0")
        assertNull(path)
    }

    @Test
    fun `version normalization - handles semver correctly`() {
        val schema = SchemaMap()
        schema["version"] = "1.2.3-alpha+build"

        // 1.2.3-alpha -> 1.2.0
        val m1 = TestMigration("1.2.0", "2.0.0")
        val engineWithMigrator = createPath(m1)

        val result = engineWithMigrator.migrate(schema, "type", "2.0.0", "type")
        assertEquals("2.0.0", result.stringOrNull("version"))
    }

    @Test
    fun `migrate - executes transformations and updates version key`() {
        val m1 = TestMigration("1.0.0", "1.1.0") { it["data"] = "processed" }
        val engine = createPath(m1)
        val schema = SchemaMap()
        schema["version"] = "1.0.0"
        schema["data"] to "raw"

        val result = engine.migrate(schema, "type", "1.1.0", "type")

        assertEquals("processed", result.stringOrNull("data"))
        assertEquals("1.1.0", result.stringOrNull("version"))
    }

    @Test
    fun `migrate - throws error when version is missing from schema`() {
        val engine = createPath()
        val schema = SchemaMap()
        schema["not-a-version"] = "1.0.0"

        val exception = assertFailsWith<IllegalStateException> {
            engine.migrate(schema, "type", "1.1.0", "type")
        }
        assertTrue(exception.message!!.contains("Version must be specified"))
    }

    @Test
    fun `migrate - throws error when path cannot be found`() {
        val engine = createPath()
        val schema = SchemaMap()
        schema["version"] = "1.0.0"

        assertFailsWith<IllegalStateException> {
            engine.migrate(schema, "type", "2.0.0", "type")
        }
    }

    private fun createPath(vararg migrations: Migration): MigrationPath {
        val map = migrations.groupBy { it.fromKey }
        return MigrationPath(map)
    }

    private class TestMigration(
        fromV: String,
        toV: String,
        fromT: String = "type",
        toT: String = "type",
        val transformation: (SchemaMap) -> Unit = {}
    ) : Migration(fromT, fromV, toT, toV) {
        override fun migrate(schema: SchemaMap): SchemaMap {
            transformation(schema)
            return schema
        }
    }
}
