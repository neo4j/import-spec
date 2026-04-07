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
package script

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TypeScriptTypesTest {

    private lateinit var tsTypes: TypeScriptTypes

    @Before
    fun setup() {
        tsTypes = TypeScriptTypes()
    }

    @Test
    fun `should replace simple field type`() {
        val input = """
            interface User {
                id: string;
                status: string;
            }
        """.trimIndent()

        tsTypes.replace("User", "status", "string", "'ACTIVE' | 'INACTIVE'")

        val result = tsTypes.run(input)

        val expected = """
            interface User {
                id: string;
                status: 'ACTIVE' | 'INACTIVE';
            }
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `should ignore identical field names at deeper nesting levels`() {
        val input = """
            interface Product {
                metadata: {
                    name: string;
                };
                name: string;
            }
        """.trimIndent()

        tsTypes.replace("Product", "name", "string", "ProductNames")

        val result = tsTypes.run(input)

        assert(result.contains("name: string;"))
        assert(result.contains("name: ProductNames;"))
    }

    @Test
    fun `should handle multiple replacements in different interfaces`() {
        val input = """
            interface Cat {
                lives: number;
            }
            interface Dog {
                bark: string;
            }
        """.trimIndent()

        tsTypes.replace("Cat", "lives", "number", "0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9")
        tsTypes.replace("Dog", "bark", "string", "'WOOF' | 'YIP'")

        val result = tsTypes.run(input)

        assert(result.contains("lives: 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9;"))
        assert(result.contains("bark: 'WOOF' | 'YIP';"))
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error when interface is missing`() {
        val input = "interface CorrectName { field: string; }"
        tsTypes.replace("WrongName", "field", "string", "union")
        tsTypes.run(input)
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw error when type mismatch occurs`() {
        val input = "interface User { age: number; }"
        // Expecting 'string' but found 'number'
        tsTypes.replace("User", "age", "string", "any")
        tsTypes.run(input)
    }

    @Test
    fun `should be idempotent (already replaced fields are skipped)`() {
        val input = "interface User { role: 'ADMIN' | 'USER'; }"

        tsTypes.replace("User", "role", "string", "'ADMIN' | 'USER'")

        val result = tsTypes.run(input)
        assertEquals(input, result) // No changes, no errors
    }

    @Test
    fun `should handle fields with generics or complex characters`() {
        val input = "interface Registry { data: Map<string, number>; }"
        tsTypes.replace("Registry", "data", "Map<string, number>", "Record<string, any>")

        val result = tsTypes.run(input)
        assert(result.contains("data: Record<string, any>;"))
    }
}
