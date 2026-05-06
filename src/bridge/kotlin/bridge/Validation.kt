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
import kotlinx.serialization.json.Json
import validate.ValidationTree
import validate.Validations
import kotlin.experimental.ExperimentalNativeApi

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("validate")
fun validate(inputJson: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val graphModel = GraphSpec.Json.decodeFromString(content = input[0])
        val validation = ValidationTree()
        validation.build(Validations.all)
        val issues = validation.validate(graphModel)
        json.encodeToString(issues)
    }
