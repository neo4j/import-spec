package model.extension

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.constraint.RelationshipConstraint
import model.constraint.RelationshipConstraintJs
import model.constraint.toClass
import model.constraint.toJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.toClass
import model.toJs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BooleanValueJsTest : JsMappingTest<BooleanValue, BooleanValueJs>() {

    override fun createClass() = BooleanValue(
        value = true
    )

    override fun toJs(k: BooleanValue): BooleanValueJs = k.toJs()

    override fun toClass(js: BooleanValueJs): BooleanValue = js.toClass()

    override fun verifyJsObject(jsObject: BooleanValueJs) {
        assertEquals("Boolean", jsObject.type)
        assertEquals(true, jsObject.value)
    }

}
