package model.node

import model.mapping.JsMappingTest
import model.extension.StringValue
import model.extension.stringValueJs
import model.type.IndexType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeIndexJsTest : JsMappingTest<NodeIndex, NodeIndexJs>() {

    override fun createClass() = NodeIndex(
        type = IndexType.TEXT,
        labels = mutableSetOf("node_label"),
        properties = mutableSetOf("property_1", "property_2"),
        options = mutableMapOf(
            "key1" to StringValue("val1")
        ),
        extensions = mutableMapOf(
            "key1" to StringValue("val1")
        )
    )

    override fun toJs(k: NodeIndex): NodeIndexJs = k.toJs()

    override fun toClass(js: NodeIndexJs): NodeIndex = js.toClass()

    override fun verifyJsObject(jsObject: NodeIndexJs) {
        assertEquals("TEXT", jsObject.type)
        val labels = jsObject.labels
        assertEquals(1, labels.size)
        assertTrue(labels.contains("node_label"))
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertJsEquals(stringValueJs("val1"), jsObject.options["key1"])
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
