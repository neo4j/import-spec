package model.extension

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

class ListValueJsTest : JsMappingTest<ListValue, ListValueJs>() {

    override fun createClass() = ListValue(
        value = mutableListOf(StringValue("val1"), LongValue(4))
    )

    override fun toJs(k: ListValue): ListValueJs = k.toJs()

    override fun toClass(js: ListValueJs): ListValue = js.toClass()

    override fun verifyJsObject(jsObject: ListValueJs) {
        assertEquals("List", jsObject.type)
        val (first, second) = jsObject.value
        assertEquals("String", first.type)
        assertEquals("val1", (first as StringValueJs).value)
        assertEquals("Long", second.type)
        assertEquals(4, (second as LongValueJs).value)
    }

}
