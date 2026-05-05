package model.node

import model.extension.StringValue
import model.extension.toJs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeIndexEditorTest {

    private fun createIndex(): NodeIndexJs = nodeIndexJs(
        type = "RANGE",
        labels = arrayOf("Person"),
        properties = arrayOf("name")
    )

    @Test
    fun testSetType() {
        val index = createIndex()
        NodeIndexEditor.setType(index, "TEXT")
        assertEquals("TEXT", index.type)
    }

    @Test
    fun testAddLabel() {
        val index = createIndex()

        // Add new label
        NodeIndexEditor.addLabel(index, "Actor")
        assertTrue(index.labels.contains("Actor"))
        assertEquals(2, index.labels.size)

        // Test Idempotency (adding existing label should do nothing)
        NodeIndexEditor.addLabel(index, "Actor")
        assertEquals(2, index.labels.size, "Should not add duplicate labels")
    }

    @Test
    fun testRemoveLabel() {
        val index = createIndex()
        index.labels = arrayOf("A", "B", "C")

        // Remove existing
        NodeIndexEditor.removeLabel(index, "B")
        assertFalse(index.labels.contains("B"))
        assertEquals(2, index.labels.size)
        assertEquals("A", index.labels[0])
        assertEquals("C", index.labels[1])

        // Remove non-existent (should not crash)
        NodeIndexEditor.removeLabel(index, "Z")
        assertEquals(2, index.labels.size)
    }

    @Test
    fun testAddProperty() {
        val index = createIndex()

        NodeIndexEditor.addProperty(index, "age")
        assertTrue(index.properties.contains("age"))

        // Test Idempotency
        NodeIndexEditor.addProperty(index, "age")
        assertEquals(2, index.properties.size)
    }

    @Test
    fun testRemoveProperty() {
        val index = createIndex()
        index.properties = arrayOf("p1", "p2")

        NodeIndexEditor.removeProperty(index, "p1")
        assertFalse(index.properties.contains("p1"))
        assertEquals(1, index.properties.size)

        // Remove non-existent
        NodeIndexEditor.removeProperty(index, "p99")
        assertEquals(1, index.properties.size)
    }

    @Test
    fun testOptionsOperations() {
        val index = createIndex()
        val dummyValue = StringValue("test").toJs()

        // Set Option
        NodeIndexEditor.setOption(index, "spatial", dummyValue)
        assertNotNull(index.options["spatial"])

        // Update Option
        val newValue = StringValue("new").toJs()
        NodeIndexEditor.setOption(index, "spatial", newValue)
        assertEquals(newValue, index.options["spatial"])

        // Remove Option
        NodeIndexEditor.removeOption(index, "spatial")
        assertNull(index.options["spatial"])
    }
}
