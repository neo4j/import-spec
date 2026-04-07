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

class TypeScriptUnionsTest {

    @Test
    fun `Add enum union`() {
        val builder = TypeScriptUnions()
        builder.union("TestType", "TestTypeJs")

        val result = builder.run(
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
    fun `Drop old unions`() {
        val builder = TypeScriptUnions()
        val result = builder.run(
            """
            export declare abstract class TestType {}
            //auto-gen
            export type TestTypeJs = "WRONG"
            """.trimIndent()
        )

        val lines = result.lines()
        assertEquals(
            """
            export declare abstract class TestType {}
            """.trimIndent(),
            lines[lines.lastIndex - 1]
        )
    }
}
