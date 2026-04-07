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
import org.junit.Test
import script.TypeScriptUnions
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TypeScriptUnionsTest {

    @Test
    fun `should ignore nested get names()'s`() {
        val unions = TypeScriptUnions()
        unions.create("TestType", "TestTypeJs")

        val result = unions.run(
            """
            export declare abstract class TestType {
                private constructor();
                static get ONE(): ConstraintType & {
                    get name(): "ONE<TYPE>";
                    get ordinal(): 0;
                };
                static get TWO(): ConstraintType & {
                    get name(): "TWO";
                    get ordinal(): 1;
                };
                static get THREE_TEST(): ConstraintType & {
                    get name(): "Three test";
                    get ordinal(): 2;
                };
                static values(): [typeof ConstraintType.ONE, typeof ConstraintType.TWO, typeof ConstraintType.THREE_TEST];
                static valueOf(value: string): ConstraintType;
                get name(): "ONE<TYPE>" | "TWO" | "Three test";
                get ordinal(): 0 | 1 | 2;
            }
            """.trimIndent()
        )

        val lines = result.lines()
        assertEquals(
            """
            export type TestTypeJs = "ONE<TYPE>" | "TWO" | "Three test"
            """.trimIndent(),
            lines[lines.lastIndex - 1]
        )
    }

    @Test
    fun `should generate union from enum class`() {
        val unions = TypeScriptUnions()
        val input = """
            abstract class Color {
                get name(): "RED" | "GREEN" | "BLUE";
            }
        """.trimIndent()

        unions.create("Color", "ColorType")
        val result = unions.run(input)

        val expected = """
            abstract class Color {
                get name(): "RED" | "GREEN" | "BLUE";
            }
            //auto-gen
            export type ColorType = "RED" | "GREEN" | "BLUE"
        """.trimIndent()

        assertEquals(expected.trim(), result.trim())
    }

    @Test
    fun `should rename values in the generated union`() {
        val unions = TypeScriptUnions()
        val input = """
            abstract class Priority {
                get name(): "HIGH" | "LOW";
            }
        """.trimIndent()

        unions.create("Priority", "PriorityLevel")
        unions.rename("PriorityLevel", mapOf("HIGH" to "URGENT", "LOW" to "RELAXED"))

        val result = unions.run(input)

        assertTrue(result.contains("export type PriorityLevel = \"URGENT\" | \"RELAXED\""))
    }

    @Test
    fun `should handle existing marker by replacing content after it`() {
        val unions = TypeScriptUnions()
        val input = """
            val x = 10;
            //auto-gen
            export type OldType = "OLD"
        """.trimIndent()

        unions.create("Status", "StatusType")

        // This input mimics the class that 'create' expects
        val classDefinition = """
            abstract class Status {
                get name(): "OPEN" | "CLOSED";
            }
        """.trimIndent()

        val result = unions.run(classDefinition + "\n" + input)

        // It should keep code before //auto-gen and discard "OldType"
        assertTrue(result.contains("val x = 10;"))
        assertTrue(result.contains("export type StatusType = \"OPEN\" | \"CLOSED\""))
        assert(!result.contains("OldType"))
    }

    @Test
    fun `should handle multiple unions`() {
        val tsUnions = TypeScriptUnions()
        val input = """
            abstract class A { get name(): "A1" | "A2"; }
            abstract class B { get name(): "B1" | "B2"; }
        """.trimIndent()

        tsUnions.create("A", "UnionA")
        tsUnions.create("B", "UnionB")

        val result = tsUnions.run(input)

        assertTrue(result.contains("export type UnionA = \"A1\" | \"A2\""))
        assertTrue(result.contains("export type UnionB = \"B1\" | \"B2\""))
    }

    @Test
    fun `should throw error if enum class is not found`() {
        val unions = TypeScriptUnions()
        val input = "abstract class Other { get name(): \"X\"; }"
        unions.create("Missing", "MissingUnion")

        assertFailsWith<IllegalStateException>("Enum Missing not found") {
            unions.run(input)
        }
    }

    @Test
    fun `should throw error if get name is missing`() {
        val unions = TypeScriptUnions()
        val input = """
            abstract class Broken {
                get value(): "X";
            }
        """.trimIndent()
        unions.create("Broken", "BrokenUnion")

        assertFailsWith<IllegalStateException>("Unable to find `get name()`") {
            unions.run(input)
        }
    }
}
