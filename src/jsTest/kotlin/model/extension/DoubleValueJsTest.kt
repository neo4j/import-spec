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

class DoubleValueJsTest : JsMappingTest<DoubleValue, DoubleValueJs>() {

    override fun createClass() = DoubleValue(
        value = 3.1415962
    )

    override fun toJs(k: DoubleValue): DoubleValueJs = k.toJs()

    override fun toClass(js: DoubleValueJs): DoubleValue = js.toClass()

    override fun verifyJsObject(jsObject: DoubleValueJs) {
        assertEquals("Double", jsObject.type)
        assertEquals(3.1415962, jsObject.value)
    }

}
