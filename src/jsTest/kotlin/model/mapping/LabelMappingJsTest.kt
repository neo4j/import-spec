package model.mapping

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

class LabelMappingJsTest : JsMappingTest<LabelMapping, LabelMappingJs>() {

    override fun createClass() = LabelMapping(
        table = "table_name",
        field = "field_name",
    )

    override fun toJs(k: LabelMapping): LabelMappingJs = k.toJs()

    override fun toClass(js: LabelMappingJs): LabelMapping = js.toClass()

    override fun verifyJsObject(jsObject: LabelMappingJs) {
        assertEquals("LabelMapping", jsObject.type)
        assertEquals("table_name", jsObject.table)
        assertEquals("field_name", jsObject.field)
    }

}
