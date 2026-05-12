package model.node

import model.mapping.JsMappingTest
import model.extension.StringValue
import model.extension.stringValueJs
import model.type.ConstraintType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeConstraintJsTest : JsMappingTest<NodeConstraint, NodeConstraintJs>() {

    override fun createClass() = NodeConstraint(
        type = ConstraintType.KEY,
        label = "node_label",
        properties = mutableSetOf("property_1", "property_2"),
        extensions = mutableMapOf(
            "key1" to StringValue("val1")
        )
    )

    override fun toJs(k: NodeConstraint): NodeConstraintJs = k.toJs()

    override fun toClass(js: NodeConstraintJs): NodeConstraint = js.toClass()

    override fun verifyJsObject(jsObject: NodeConstraintJs) {
        assertEquals("KEY", jsObject.type)
        assertEquals("node_label", jsObject.label)
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
