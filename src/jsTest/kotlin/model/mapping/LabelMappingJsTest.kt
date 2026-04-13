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
