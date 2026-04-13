package model

import js.objects.Record
import js.objects.buildRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {

    @Test
    fun testRecordToMap() {
        val jsObj: Record<String, Int> = buildRecord {
            this["a"] = 1
            this["b"] = 2
        }

        val result = jsObj.toMap()

        assertEquals(2, result.size)
        assertEquals(1, result["a"])
        assertEquals(2, result["b"])
    }

    @Test
    fun testRecordAssociateBy() {
        val jsObj: Record<String, Int> = buildRecord {
            this["val1"] = 10
            this["val2"] = 20
        }

        // Transform: multiply value by 2 and return as String
        val result: Map<String, String> = jsObj.associateBy { key, value ->
            (value * 2).toString()
        }

        assertEquals(2, result.size)
        assertEquals("20", result["val1"])
        assertEquals("40", result["val2"])
    }

    @Test
    fun testMapToRecordAssociateBy() {
        val kotlinMap = mapOf(
            "item1" to 5,
            "item2" to 10
        )

        val result: Record<String, Int> = kotlinMap.associateBy { key, value ->
            value + 1
        }

        // Verify using JSON stringify to ensure it's a plain JS object
        val json = JSON.stringify(result)
        assertTrue(json.contains("\"item1\":6"))
        assertTrue(json.contains("\"item2\":11"))

        assertEquals(6, result["item1"])
        assertEquals(11, result["item2"])
    }

    @Test
    fun testEmptyConversions() {
        val emptyJs: Record<String, String> = buildRecord {}
        val mapResult = emptyJs.toMap()
        assertTrue(mapResult.isEmpty())

        val emptyMap = emptyMap<String, String>()
        val jsResult = emptyMap.associateBy { k, v -> v }
        assertEquals("{}", JSON.stringify(jsResult))
    }

    @Test
    fun testComplexPolymorphicObjects() {
        val jsObj: Record<String, dynamic> = buildRecord {
            this["ext1"] = jso {
                this.type = "string"
                this.value = "hello"
            }
        }

        val result = jsObj.toMap()

        val ext1 = result["ext1"]
        assertEquals("string", ext1.type)
        assertEquals("hello", ext1.value)
    }
}
