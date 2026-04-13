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

class LongValueJsTest : JsMappingTest<LongValue, LongValueJs>() {

    override fun createClass() = LongValue(
        value = 31415962
    )

    override fun toJs(k: LongValue): LongValueJs = k.toJs()

    override fun toClass(js: LongValueJs): LongValue = js.toClass()

    override fun verifyJsObject(jsObject: LongValueJs) {
        assertEquals("Long", jsObject.type)
        assertEquals(31415962, jsObject.value)
    }

}
