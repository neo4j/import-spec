package bridge

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class ValidationTest {

    @Test
    fun testValidate() {
        val input = "{\"version\":\"1.0.0\",\"nodes\":{\"n\":{\"constraints\":{\"c1\":{\"type\":\"TYPE\",\"label\":\"L\",\"properties\":[]}}}}}".trimIndent()
        val bufferSize = 1024

        memScoped {
            val inputPtr = input.cstr.getPointer(this)
            val outputBuffer = allocArray<ByteVar>(bufferSize)

            val resultSize = validate(inputPtr, outputBuffer = outputBuffer, bufferSize = bufferSize)
            assertTrue(resultSize > 0)
            val response = Json.decodeFromString(BridgeResponse.serializer(), outputBuffer.toKString())
            assertNull(response.error)
            assertNotNull(response.data)
            assertTrue(response.data.contains("Node type constraint 'c1' must have exactly one property"))
        }
    }
}
