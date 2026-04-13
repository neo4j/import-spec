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
package validate

import model.GraphModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ValidationTreeTest {

    private val emptyModel = GraphModel(version = "1.0")

    @Test
    fun `validate throws exception if build was not called`() {
        val tree = ValidationTree()
        assertFailsWith<IllegalArgumentException> {
            tree.validate(emptyModel)
        }
    }

    @Test
    fun `single validator without dependencies is executed`() {
        val tree = ValidationTree()
        val v1 = MockValidation("V1")

        tree.build(listOf(v1))
        val issues = tree.validate(emptyModel)

        assertEquals(1, v1.callCount)
        assertTrue(issues.isEmpty())
    }

    @Test
    fun `child validator is executed if parent succeeds`() {
        val tree = ValidationTree()
        val parent = MockValidation("Parent", shouldFail = false)
        val child = MockValidation("Child", dependencies = listOf(parent))

        tree.build(listOf(parent, child))
        tree.validate(emptyModel)

        assertEquals(1, parent.callCount)
        assertEquals(1, child.callCount)
    }

    @Test
    fun `child validator is skipped if parent fails`() {
        val tree = ValidationTree()
        val parent = MockValidation("Parent", shouldFail = true)
        val child = MockValidation("Child", dependencies = listOf(parent))

        tree.build(listOf(parent, child))
        val issues = tree.validate(emptyModel)

        assertEquals(1, parent.callCount)
        assertEquals(0, child.callCount, "Child should be skipped because parent failed")
        assertEquals(1, issues.size)
        assertEquals("Parent", issues[0].code)
    }

    @Test
    fun `multiple roots are all executed`() {
        val tree = ValidationTree()
        val root1 = MockValidation("R1")
        val root2 = MockValidation("R2")

        tree.build(listOf(root1, root2))
        tree.validate(emptyModel)

        assertEquals(1, root1.callCount)
        assertEquals(1, root2.callCount)
    }

    @Test
    fun `branching dependencies - one parent failure stops entire branch`() {
        val tree = ValidationTree()
        val root = MockValidation("Root", shouldFail = true)
        val child1 = MockValidation("C1", dependencies = listOf(root))
        val child2 = MockValidation("C2", dependencies = listOf(root))

        tree.build(listOf(root, child1, child2))
        tree.validate(emptyModel)

        assertEquals(1, root.callCount)
        assertEquals(0, child1.callCount)
        assertEquals(0, child2.callCount)
    }

    @Test
    fun `deep dependency chain works correctly`() {
        val tree = ValidationTree()
        val v1 = MockValidation("V1")
        val v2 = MockValidation("V2", dependencies = listOf(v1))
        val v3 = MockValidation("V3", dependencies = listOf(v2))

        tree.build(listOf(v1, v2, v3))
        tree.validate(emptyModel)

        assertEquals(1, v1.callCount)
        assertEquals(1, v2.callCount)
        assertEquals(1, v3.callCount)
    }

    @Test
    fun `rebuild wipes previous state`() {
        val tree = ValidationTree()
        val v1 = MockValidation("V1")
        val v2 = MockValidation("V2")

        tree.build(listOf(v1))

        // Rebuild with only v2
        tree.build(listOf(v2))
        tree.validate(emptyModel)

        assertEquals(0, v1.callCount)
        assertEquals(1, v2.callCount)
    }

    @Test
    fun `validators can have multiple parents`() {
        // This tests the logic: tree.getOrPut(dependent).add(validation)
        val tree = ValidationTree()
        val p1 = MockValidation("P1")
        val p2 = MockValidation("P2")
        val child = MockValidation("Child", dependencies = listOf(p1, p2))

        tree.build(listOf(p1, p2, child))
        tree.validate(emptyModel)

        // The current implementation will run 'child' twice because it's added
        // to the 'tree' map under both P1 and P2 keys.
        assertEquals(1, p1.callCount)
        assertEquals(1, p2.callCount)
        assertEquals(2, child.callCount)
    }

    private class MockValidation(
        val name: String,
        private val dependencies: List<Validation> = emptyList(),
        private val shouldFail: Boolean = false
    ) : Validation {
        var callCount = 0

        override fun dependsOn() = dependencies

        override fun validate(model: GraphModel, issues: MutableList<Issue>) {
            callCount++
            if (shouldFail) {
                issues.add(Issue(code = name, message = "Failure in $name"))
            }
        }
    }
}
