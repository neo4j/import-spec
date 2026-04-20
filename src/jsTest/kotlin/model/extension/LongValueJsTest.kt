package model.extension

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

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
