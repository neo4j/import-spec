package model.index

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.constraint.NodeConstraint
import model.constraint.NodeConstraintJs
import model.constraint.toClass
import model.constraint.toJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.toClass
import model.toJs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipIndexJsTest : JsMappingTest<RelationshipIndex, RelationshipIndexJs>() {

    override fun createClass() = RelationshipIndex(
        type = "INDEX_TYPE",
        properties = setOf("property_1", "property_2"),
        options = mutableMapOf(
            "key1" to StringValue("val1")
        ),
        extensions = mutableMapOf(
            "key1" to StringValue("val1")
        )
    )

    override fun toJs(k: RelationshipIndex): RelationshipIndexJs = k.toJs()

    override fun toClass(js: RelationshipIndexJs): RelationshipIndex = js.toClass()

    override fun verifyJsObject(jsObject: RelationshipIndexJs) {
        assertEquals("INDEX_TYPE", jsObject.type)
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertJsEquals(stringValueJs("val1"), jsObject.options["key1"])
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
