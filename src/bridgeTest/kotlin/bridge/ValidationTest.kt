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
        val input = """{
            "version": "1.0.0",
            "nodes": {
                "n": {
                    "constraints": {
                        "c1": {
                            "type": "EXISTS",
                            "label": "L",
                            "properties": []
                        }
                    }
                }
            }
        }
        """.trimIndent()
        val bufferSize = 1024

        memScoped {
            val inputPtr = input.cstr.getPointer(this)
            val outputBuffer = allocArray<ByteVar>(bufferSize)

            val resultSize = validate(inputPtr, outputBuffer = outputBuffer, bufferSize = bufferSize)
            assertTrue(resultSize > 0)
            val response = Json.decodeFromString(BridgeResponse.serializer(), outputBuffer.toKString())
            assertNull(response.error)
            assertNotNull(response.data)
            assertTrue(response.data.contains("Node existence constraint 'c1' must have exactly one property"))
        }
    }
}
