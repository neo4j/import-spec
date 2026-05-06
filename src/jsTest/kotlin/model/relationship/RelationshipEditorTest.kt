package model.relationship

import kotlin.test.*
import js.objects.Record
import js.objects.get
import js.objects.recordOf
import model.GraphModelJs
import model.extension.StringValue
import model.extension.toJs
import model.graphModelJs

class RelationshipEditorTest {

    private lateinit var model: GraphModelJs
    private val relId = "rel-123"

    @BeforeTest
    fun setup() {
        val relationship = relationshipJs(
            type = "WORKS_AT",
            name = "WorksAt",
            id = relId
        )
        model = graphModelJs(
            version = "1.0.0",
            relationships = recordOf(relId to relationship)
        )
    }

    @Test
    fun testBasicMetadataUpdates() {
        val rel = relationshipJs(type = "A", name = "NameA", id = "1")

        RelationshipEditor.setType(rel, "B")
        RelationshipEditor.setName(rel, "NameB")

        assertEquals("B", rel.type)
        assertEquals("NameB", rel.name)
    }

    @Test
    fun testSourceTargetDelegation() {
        val rel = relationshipJs(type = "T", name = "N", id = "1")

        // Test Source (from) delegation
        RelationshipEditor.setSourceNode(rel, "Node1")
        assertEquals("Node1", rel.from.node)
        assertEquals("", rel.from.label) // RelationshipTargetEditor logic: setting node clears label

        RelationshipEditor.setSourceLabel(rel, "Label1")
        assertEquals("Label1", rel.from.label)
        assertEquals("", rel.from.node) // RelationshipTargetEditor logic: setting label clears node

        // Test Target (to) delegation
        RelationshipEditor.setTargetProperty(rel, "propX")
        assertEquals("propX", rel.to.property)
    }

    @Test
    fun testPropertyManagement() {
        val rel = model.relationships[relId]!!

        // Add Property
        val propId = RelationshipEditor.addProperty(model, relId)
        assertNotNull(rel.properties[propId])
        assertEquals(propId, rel.properties[propId]?.name)

        // Update Property
        RelationshipEditor.setPropertyName(model, relId, propId, "NewName")
        RelationshipEditor.setPropertyType(model, relId, propId, "STRING")
        RelationshipEditor.setPropertyNullable(model, relId, propId, true)

        val prop = rel.properties[propId]!!
        assertEquals("NewName", prop.name)
        assertEquals("STRING", prop.type)
        assertTrue(prop.nullable)

        // Remove Property
        RelationshipEditor.removeProperty(model, relId, propId)
        assertNull(rel.properties[propId])
    }

    @Test
    fun testConstraintManagement() {
        // Add Constraint
        val constraintId = RelationshipEditor.addConstraint(
            model, relId, type = "UNIQUE", properties = arrayOf("p1")
        )
        val constraint = model.relationships[relId]!!.constraints[constraintId]!!
        assertEquals("UNIQUE", constraint.type)
        assertTrue(constraint.properties.contains("p1"))

        // Modify Constraint Properties
        RelationshipEditor.addConstraintProperty(model, relId, constraintId, "p2")
        assertTrue(constraint.properties.contains("p2"))
        assertEquals(2, constraint.properties.size)

        RelationshipEditor.removeConstraintProperty(model, relId, constraintId, "p1")
        assertFalse(constraint.properties.contains("p1"))

        // Modify Options
        val dummy = StringValue("val").toJs()
        RelationshipEditor.setConstraintOption(model, relId, constraintId, "key1", dummy)
        assertEquals(dummy, constraint.options["key1"])

        RelationshipEditor.removeConstraintOption(model, relId, constraintId, "key1")
        assertNull(constraint.options["key1"])
    }

    @Test
    fun testIndexManagement() {
        // Add Index
        val indexId = RelationshipEditor.addIndex(model, relId, "RANGE", arrayOf("a"))
        val index = model.relationships[relId]!!.indexes[indexId]!!

        // Update Type
        RelationshipEditor.setIndexType(model, relId, indexId, "POINT")
        assertEquals("POINT", index.type)

        // Add Property
        RelationshipEditor.addIndexProperty(model, relId, indexId, "b")
        assertEquals(2, index.properties.size)

        // Set Option
        val optVal = StringValue("spatial").toJs()
        RelationshipEditor.setIndexOption(model, relId, indexId, "type", optVal)
        assertEquals(optVal, index.options["type"])
    }

    @Test
    fun testErrorHandling() {
        // Test that operations on non-existent relationships throw as expected
        assertFails {
            RelationshipEditor.setType(relationshipJs(type="", name="", id=""), "new")
            // The above won't fail, but the Model ones will:
            RelationshipEditor.addProperty(model, "non-existent-id")
        }
    }
}
