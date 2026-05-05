package model.node

import kotlin.test.*

class NodeConstraintEditorTest {

    private fun createConstraint(): NodeConstraintJs = nodeConstraintJs(
        type = "UNIQUENESS",
        label = "User",
        properties = arrayOf("email")
    )

    @Test
    fun testSetType() {
        val constraint = createConstraint()
        NodeConstraintEditor.setType(constraint, "NODE_PROPERTY_EXISTENCE")
        assertEquals("NODE_PROPERTY_EXISTENCE", constraint.type)
    }

    @Test
    fun testSetLabel() {
        val constraint = createConstraint()

        // Change label
        NodeConstraintEditor.setLabel(constraint, "Admin")
        assertEquals("Admin", constraint.label)

        // Set to null
        NodeConstraintEditor.setLabel(constraint, null)
        assertNull(constraint.label)
    }

    @Test
    fun testPropertyOperations() {
        val constraint = createConstraint()

        // Add new property
        NodeConstraintEditor.addProperty(constraint, "username")
        assertTrue(constraint.properties.contains("username"))
        assertEquals(2, constraint.properties.size)

        // Add duplicate (should be ignored)
        NodeConstraintEditor.addProperty(constraint, "username")
        assertEquals(2, constraint.properties.size)

        // Remove existing property
        NodeConstraintEditor.removeProperty(constraint, "email")
        assertFalse(constraint.properties.contains("email"))
        assertEquals(1, constraint.properties.size)
        assertEquals("username", constraint.properties[0])

        // Remove non-existent property (should not crash)
        NodeConstraintEditor.removeProperty(constraint, "missing")
        assertEquals(1, constraint.properties.size)
    }
}
