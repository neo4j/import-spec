package model.relationship

import js.objects.Object.Companion.keys
import kotlin.test.*
import model.extension.StringValue
import model.extension.toJs

class RelationshipConstraintEditorTest {

    @Test
    fun testFactoryFunctionInitialization() {
        val type = "RELATIONSHIP_TYPE"
        val properties = arrayOf("prop1", "prop2")

        val constraint = relationshipConstraintJs(
            type = type,
            properties = properties
        )

        assertEquals(type, constraint.type)
        assertEquals(2, constraint.properties.size)
        assertTrue(constraint.properties.contains("prop1"))
        assertEquals(0, keys(constraint.options).size)
        assertEquals(0, keys(constraint.extensions).size)
    }

    @Test
    fun testSetType() {
        val constraint = relationshipConstraintJs(type = "OLD_TYPE")
        RelationshipConstraintEditor.setType(constraint, "NEW_TYPE")

        assertEquals("NEW_TYPE", constraint.type)
    }

    @Test
    fun testAddProperty() {
        val constraint = relationshipConstraintJs(type = "test")

        // Add first property
        RelationshipConstraintEditor.addProperty(constraint, "id")
        assertEquals(1, constraint.properties.size)
        assertEquals("id", constraint.properties[0])

        // Add second property
        RelationshipConstraintEditor.addProperty(constraint, "name")
        assertEquals(2, constraint.properties.size)
        assertTrue(constraint.properties.contains("name"))

        // Test Duplicate Prevention: Adding "id" again should not increase size
        RelationshipConstraintEditor.addProperty(constraint, "id")
        assertEquals(2, constraint.properties.size, "Should not add duplicate properties")
    }

    @Test
    fun testRemoveProperty() {
        val constraint = relationshipConstraintJs(
            type = "test",
            properties = arrayOf("a", "b", "c")
        )

        // Remove middle element
        RelationshipConstraintEditor.removeProperty(constraint, "b")
        assertEquals(2, constraint.properties.size)
        assertFalse(constraint.properties.contains("b"))
        assertEquals("a", constraint.properties[0])
        assertEquals("c", constraint.properties[1])

        // Remove non-existent element
        RelationshipConstraintEditor.removeProperty(constraint, "non-existent")
        assertEquals(2, constraint.properties.size)
    }

    @Test
    fun testSetOption() {
        val constraint = relationshipConstraintJs(type = "test")
        val val1 = StringValue("value1").toJs()
        val val2 = StringValue("value2").toJs()

        // Set new option
        RelationshipConstraintEditor.setOption(constraint, "limit", val1)
        assertEquals(val1, constraint.options["limit"])

        // Update existing option
        RelationshipConstraintEditor.setOption(constraint, "limit", val2)
        assertEquals(val2, constraint.options["limit"])
        assertEquals(1, keys(constraint.options).size)
    }

    @Test
    fun testRemoveOption() {
        val constraint = relationshipConstraintJs(type = "test")
        val val1 = StringValue("value1").toJs()

        RelationshipConstraintEditor.setOption(constraint, "timeout", val1)
        assertTrue(keys(constraint.options).contains("timeout"))

        // Remove existing
        RelationshipConstraintEditor.removeOption(constraint, "timeout")
        assertFalse(keys(constraint.options).contains("timeout"))
        assertEquals(0, keys(constraint.options).size)

        // Remove non-existent (should not throw)
        RelationshipConstraintEditor.removeOption(constraint, "anything")
    }

    @Test
    fun testEmptyDefaults() {
        val constraint = relationshipConstraintJs(type = "minimal")

        assertNotNull(constraint.properties)
        assertNotNull(constraint.options)
        assertNotNull(constraint.extensions)
        assertEquals(0, constraint.properties.size)
    }
}
