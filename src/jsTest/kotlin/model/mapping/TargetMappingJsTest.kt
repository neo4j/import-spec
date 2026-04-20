package model.mapping

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

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
