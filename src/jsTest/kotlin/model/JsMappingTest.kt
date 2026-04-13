package model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class JsMappingTest<K, J> {

    abstract fun createClass(): K

    abstract fun toJs(k: K): J
    abstract fun toClass(js: J): K

    open fun verifyJsObject(jsObject: J) {}

    @Test
    fun testRoundTrip() {
        val original = createClass()

        val jsObject = toJs(original)
        verifyJsObject(jsObject)
        val mappedBack = toClass(jsObject)

        assertEquals(original, mappedBack, "The round-trip conversion failed to maintain data integrity.")
    }

    fun assertJsEquals(expected: Any?, actual: Any?, message: String? = null) {
        val expectedJson = JSON.stringify(expected)
        val actualJson = JSON.stringify(actual)
        assertEquals(expectedJson, actualJson, message)
    }
}
