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

class StringValueJsTest : JsMappingTest<StringValue, StringValueJs>() {

    override fun createClass() = StringValue(
        value = "31415962"
    )

    override fun toJs(k: StringValue): StringValueJs = k.toJs()

    override fun toClass(js: StringValueJs): StringValue = js.toClass()

    override fun verifyJsObject(jsObject: StringValueJs) {
        assertEquals("String", jsObject.type)
        assertEquals("31415962", jsObject.value)
    }

}
