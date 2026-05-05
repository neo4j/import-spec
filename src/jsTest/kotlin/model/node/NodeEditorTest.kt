package model.node

import js.objects.recordOf
import model.GraphModelJs
import model.extension.StringValue
import model.extension.toJs
import model.graphModelJs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NodeEditorTest {

    private lateinit var model: GraphModelJs
    private val nodeId = "node-1"

    @BeforeTest
    fun setup() {
        val initialNode = nodeJs(
            name = "Initial Node",
            id = nodeId
        )

        model = graphModelJs(
            version = "1.0",
            nodes = recordOf(nodeId to initialNode),
        )
    }

    @Test
    fun testSetName() {
        NodeEditor.setName(model, nodeId, "Updated Name")
        assertEquals("Updated Name", model.nodes[nodeId]?.name)
    }

    @Test
    fun testLabelOperations() {
        // Test Identifying Label
        NodeEditor.setIdentifyingLabel(model, nodeId, "Person")
        assertEquals("Person", model.nodes[nodeId]?.labels?.identifier)

        // Test Implied Labels
        NodeEditor.addImpliedLabel(model, nodeId, "Entity")
        assertEquals(true, model.nodes[nodeId]?.labels?.implied?.contains("Entity"))

        NodeEditor.removeImpliedLabel(model, nodeId, "Entity")
        assertNotEquals(true, model.nodes[nodeId]?.labels?.implied?.contains("Entity"))

        // Test Optional Labels
        NodeEditor.addOptionalLabel(model, nodeId, "Student")
        assertEquals(true, model.nodes[nodeId]?.labels?.optional?.contains("Student"))

        NodeEditor.removeOptionalLabel(model, nodeId, "Student")
        assertNotEquals(true, model.nodes[nodeId]?.labels?.optional?.contains("Student"))
    }

    @Test
    fun testPropertyLifecycle() {
        // Add Property
        val propId = NodeEditor.addProperty(model, nodeId)
        assertNotNull(model.nodes[nodeId]?.properties?.get(propId))

        // Set Property Attributes
        NodeEditor.setPropertyName(model, nodeId, propId, "age")
        NodeEditor.setPropertyType(model, nodeId, propId, "Integer")
        NodeEditor.setPropertyNullable(model, nodeId, propId, false)
        NodeEditor.setPropertyUnique(model, nodeId, propId, true)

        val prop = model.nodes[nodeId]?.properties?.get(propId)
        assertEquals("age", prop?.name)

        // Remove Property
        NodeEditor.removeProperty(model, nodeId, propId)
        assertNull(model.nodes[nodeId]?.properties?.get(propId))
    }

    @Test
    fun testConstraintOperations() {
        val constraintId = NodeEditor.addConstraint(
            model = model,
            nodeId = nodeId,
            type = "UNIQUENESS",
            label = "User"
        )

        val node = model.nodes[nodeId]!!
        assertNotNull(node.constraints[constraintId])
        assertEquals("UNIQUENESS", node.constraints[constraintId]?.type)

        // Update Label
        NodeEditor.setConstraintLabel(model, nodeId, constraintId, "Admin")
        assertEquals("Admin", node.constraints[constraintId]?.label)

        // Update Type
        NodeEditor.setConstraintType(model, nodeId, constraintId, "NODE_KEY")
        assertEquals("NODE_KEY", node.constraints[constraintId]?.type)

        // Property Management
        NodeEditor.addConstraintProperty(model, nodeId, constraintId, "email")
        assertEquals(true, node.constraints[constraintId]?.properties?.contains("email"))

        NodeEditor.removeConstraintProperty(model, nodeId, constraintId, "email")
        assertNotEquals(true, node.constraints[constraintId]?.properties?.contains("email"))
    }

    @Test
    fun testIndexOperations() {
        val indexId = NodeEditor.addIndex(
            model = model,
            nodeId = nodeId,
            type = "RANGE",
            labels = arrayOf("User")
        )

        val node = model.nodes[nodeId]!!
        assertNotNull(node.indexes[indexId])

        // Test Type
        NodeEditor.setIndexType(model, nodeId, indexId, "TEXT")
        assertEquals("TEXT", node.indexes[indexId]?.type)

        // Test Labels
        NodeEditor.addIndexLabel(model, nodeId, indexId, "Account")
        assertEquals(true, node.indexes[indexId]?.labels?.contains("Account"))

        NodeEditor.removeIndexLabel(model, nodeId, indexId, "User")
        assertNotEquals(true, node.indexes[indexId]?.labels?.contains("User"))

        // Test Properties
        NodeEditor.addIndexProperty(model, nodeId, indexId, "username")
        assertEquals(true, node.indexes[indexId]?.properties?.contains("username"))

        NodeEditor.removeIndexProperty(model, nodeId, indexId, "username")
        assertNotEquals(true, node.indexes[indexId]?.properties?.contains("username"))

        // Test Options (ExtensionValueJs)
        val dummyValue = StringValue("test").toJs()
        NodeEditor.setIndexOption(model, nodeId, indexId, "analyzer", dummyValue)
        assertNotNull(node.indexes[indexId]?.options?.get("analyzer"))

        NodeEditor.removeIndexOption(model, nodeId, indexId, "analyzer")
        assertNull(node.indexes[indexId]?.options?.get("analyzer"))
    }

    @Test
    fun testErrorHandling() {
        // Verify that passing a non-existent nodeId throws an exception (via getOrThrow)
        assertFails {
            NodeEditor.setName(model, "non-existent-id", "New Name")
        }

        // Verify that passing a non-existent propertyId throws an exception
        assertFails {
            NodeEditor.setPropertyName(model, nodeId, "fake-prop", "name")
        }
    }
}
