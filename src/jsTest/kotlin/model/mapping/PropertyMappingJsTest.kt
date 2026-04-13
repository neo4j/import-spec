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

class PropertyMappingJsTest : JsMappingTest<PropertyMapping, PropertyMappingJs>() {

    override fun createClass() = PropertyMapping(
        field = "field_name",
    )

    override fun toJs(k: PropertyMapping): PropertyMappingJs = k.toJs()

    override fun toClass(js: PropertyMappingJs): PropertyMapping = js.toClass()

    override fun verifyJsObject(jsObject: PropertyMappingJs) {
        assertEquals("field_name", jsObject.field)
    }

}
