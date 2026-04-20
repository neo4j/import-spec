package model.extension

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

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
