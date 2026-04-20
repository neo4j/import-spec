package model.relationship

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

class RelationshipTargetJsTest : JsMappingTest<RelationshipTarget, RelationshipTargetJs>() {

    override fun createClass() = RelationshipTarget(
        node = "nodeId",
        label = "label",
        property = "property",
    )

    override fun toJs(k: RelationshipTarget): RelationshipTargetJs = k.toJs()

    override fun toClass(js: RelationshipTargetJs): RelationshipTarget = js.toClass()

    override fun verifyJsObject(jsObject: RelationshipTargetJs) {
        assertEquals("nodeId", jsObject.node)
        assertEquals("label", jsObject.label)
        assertEquals("property", jsObject.property)
    }

}
