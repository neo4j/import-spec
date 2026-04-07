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
import script.indexOfAtDepth
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTest {

    @Test
    fun `User specific case - Find bracket at depth 0`() {
        val result = indexOfAtDepth("test {}", "}", targetDepth = 0, startIndex = 5)
        assertEquals(6, result)
    }

    @Test
    fun `TypeScript scenario - Ignore nested depth`() {
        val code = """
            export abstract class TestType {
                static get ONE(): {
                    get name(): "ONE";
                };
                get name(): "TARGET";
            }
        """.trimIndent()

        val result = indexOfAtDepth(code, "get name():", targetDepth = 1)

        val foundFragment = code.substring(result, result + 20)
        assert(foundFragment.contains("TARGET"))
    }

    @Test
    fun `Find term at depth 0`() {
        val code = "val x = 1; { val x = 2; }"
        assertEquals(0, indexOfAtDepth(code, "val x", targetDepth = 0))
    }

    @Test
    fun `Find term at depth 1`() {
        val code = "val x = 1; { val x = 2; }"
        assertEquals(13, indexOfAtDepth(code, "val x", targetDepth = 1))
    }

    @Test
    fun `Return -1 if term is at wrong depth`() {
        val code = "class A { get name() }"
        assertEquals(-1, indexOfAtDepth(code, "class", targetDepth = 1))
    }

    @Test
    fun `Find opening bracket as search term`() {
        val code = "outer { inner }"
        assertEquals(6, indexOfAtDepth(code, "{", targetDepth = 0))
    }
}
