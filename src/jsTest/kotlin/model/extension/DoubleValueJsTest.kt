package model.extension

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

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
