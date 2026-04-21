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
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.posix.memcpy

@Serializable
data class BridgeResponse(
    val data: String? = null,
    val error: String? = null
)

/*
Handles the Kotlin/Native bridge operations to safely invoke Kotlin methods from C.
Converts the input to a Kotlin string, invokes the passed [action] and writes the
output to the provided output buffer in a memory safe way.
 */
@OptIn(ExperimentalForeignApi::class)
fun invokeBridge(
    input: CPointer<ByteVar>?,
    outBuffer: CPointer<ByteVar>?,
    bufferSize: Int,
    action: (String) -> String,
): Int {
    if (input == null || outBuffer == null || bufferSize < 1) return -1

    val response = runCatching {
        action(input.toKString())
    }.fold(
        onSuccess = { data ->
            BridgeResponse(data = data)
        },
        onFailure = { exception ->
            BridgeResponse(error = exception.message ?: "Unknown Kotlin error")
        }
    )

    val jsonResp = Json.encodeToString(response)


    // we use memScoped here to ensure that we don't need to do any manual memory management
    // .cstr and getPointer() will allocate memory outside of Kotlin's GC scope, but memScoped
    // automatically handles the freeing of memory when the block exits
    return memScoped {
        // convert to c-compatible null-terminated utf-8 string
        val cstr = jsonResp.cstr

        // check that the output buffer is large enough before writing as memScoped is a blunt tool
        // we return the negative required size if buffer insufficient to allow caller to adjust
        if (bufferSize < cstr.size) return@memScoped -cstr.size

        // write to output buffer
        memcpy(outBuffer, cstr.getPointer(this), cstr.size.toULong())

        // return the number of bytes the caller should actually care about (the JSON length)
        // cstr.size includes the null terminator, so size - 1 is the JSON text length
        cstr.size - 1
    }
}
