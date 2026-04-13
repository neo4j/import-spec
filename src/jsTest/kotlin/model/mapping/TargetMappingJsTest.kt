package model.mapping

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.constraint.NodeConstraint
import model.constraint.NodeConstraintJs
import model.constraint.toClass
import model.constraint.toJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.index.RelationshipIndex
import model.index.RelationshipIndexJs
import model.index.toClass
import model.index.toJs
import model.toClass
import model.toJs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TargetMappingJsTest : JsMappingTest<TargetMapping, TargetMappingJs>() {

    override fun createClass() = TargetMapping(
        node = "nodeId",
        label = "label",
        properties = mapOf("prop" to PropertyMapping("field")),
    )

    override fun toJs(k: TargetMapping): TargetMappingJs = k.toJs()

    override fun toClass(js: TargetMappingJs): TargetMapping = js.toClass()

    override fun verifyJsObject(jsObject: TargetMappingJs) {
        assertEquals("nodeId", jsObject.node)
        assertEquals("label", jsObject.label)
        assertJsEquals(propertyMappingJs("field"), jsObject.properties["prop"])
    }

}
