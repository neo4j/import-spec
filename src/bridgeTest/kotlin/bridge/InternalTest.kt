/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bridge

import codec.format.JsonFormat
import codec.schema.SchemaMap
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class BridgeTest {

    @Test
    fun testSuccessfulBridgeCall() {
        val input = "{\"version\":\"1\", \"data\":\"test\"}"
        val bufferSize = 1024

        memScoped {
            val inputPtr = input.cstr.getPointer(this)
            val outputBuffer = allocArray<ByteVar>(bufferSize)
            val format = JsonFormat.build()

            val resultSize = invokeBridge(inputPtr, outputBuffer, bufferSize) {
                val schema = format.decodeFromString(it) as SchemaMap
                schema["version"] = "2"
                format.encodeToString(schema)
            }

            assertTrue(resultSize > 0)
            val response = Json.decodeFromString(BridgeResponse.serializer(), outputBuffer.toKString())
            assertNull(response.error)
            assertNotNull(response.data)
            val output = format.decodeFromString(response.data) as SchemaMap
            assertEquals(output["data"].toString(), "test")
            assertEquals(output["version"].toString(), "2")
        }
    }

    @Test
    fun testNullInputs() {
        val input = "{\"version\":\"1\", \"data\":\"test\"}"
        val bufferSize = 1024

        memScoped {
            val inputPtr = input.cstr.getPointer(this)
            val outputBuffer = allocArray<ByteVar>(bufferSize)

            var resultSize = invokeBridge(null, outputBuffer, bufferSize) { it }
            assertEquals(resultSize, -1)

            resultSize = invokeBridge(inputPtr, null, bufferSize) { it }
            assertEquals(resultSize, -1)

            resultSize = invokeBridge(inputPtr, outputBuffer, -1) { it }
            assertEquals(resultSize, -1)
        }
    }

    @Test
    fun testBufferTooSmall() {
        val input = "{\"version\":\"1\", \"data\":\"test\"}"
        val bufferSize = 5 // Purposely too small for result to be written to

        memScoped {
            val inputPtr = input.cstr.getPointer(this)
            val outputBuffer = allocArray<ByteVar>(bufferSize)

            val resultSize = invokeBridge(inputPtr, outputBuffer, bufferSize) { it }

            // Should return a negative number indicating the required size
            // As the response is wrapped in a BridgeResponse model it is bigger than input
            assertTrue(resultSize <= input.length * -1)
        }
    }

    @Test
    fun testKotlinExceptionThrownInAction() {
        val input = "{\"version\":\"1\", \"data\":\"test\"}"
        val bufferSize = 1024

        memScoped {
            val inputPtr = input.cstr.getPointer(this)
            val outputBuffer = allocArray<ByteVar>(bufferSize)

            val resultSize = invokeBridge(inputPtr, outputBuffer, bufferSize) {
                throw RuntimeException("Something went wrong")
            }

            assertTrue(resultSize > 0)
            val response = Json.decodeFromString(BridgeResponse.serializer(), outputBuffer.toKString())
            assertNull(response.data)
            assertNotNull(response.error)
            assertContains(response.error, "Something went wrong")
        }
    }
}
