package model.extension

import model.mapping.JsMappingTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MapValueJsTest : JsMappingTest<MapValue, MapValueJs>() {

    override fun createClass() = MapValue(
        value = mutableMapOf("key1" to StringValue("val1"), "key2" to LongValue(4))
    )

    override fun toJs(k: MapValue): MapValueJs = k.toJs()

    override fun toClass(js: MapValueJs): MapValue = js.toClass()

    override fun verifyJsObject(jsObject: MapValueJs) {
        assertEquals("Map", jsObject.type)
        val first = jsObject.value["key1"]
        val second = jsObject.value["key2"]
        assertNotNull(first)
        assertNotNull(second)
        assertEquals("String", first.type)
        assertEquals("val1", (first as StringValueJs).value)
        assertEquals("Long", second.type)
        assertEquals(4, (second as LongValueJs).value)
    }

}
