package model.constraint

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.toClass
import model.toJs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeConstraintJsTest : JsMappingTest<NodeConstraint, NodeConstraintJs>() {

    override fun createClass() = NodeConstraint(
        type = "CONSTRAINT_TYPE",
        label = "node_label",
        properties = setOf("property_1", "property_2"),
        extensions = mutableMapOf(
            "key1" to StringValue("val1")
        )
    )

    override fun toJs(k: NodeConstraint): NodeConstraintJs = k.toJs()

    override fun toClass(js: NodeConstraintJs): NodeConstraint = js.toClass()

    override fun verifyJsObject(jsObject: NodeConstraintJs) {
        assertEquals("CONSTRAINT_TYPE", jsObject.type)
        assertEquals("node_label", jsObject.label)
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
