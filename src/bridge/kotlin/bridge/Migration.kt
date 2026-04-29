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

import GraphSpec
import codec.format.JsonFormat
import codec.schema.SchemaMap
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import migrate.MigrationPath
import kotlin.experimental.ExperimentalNativeApi

private val format = JsonFormat(Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
})

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("migrate")
fun migrate(inputJson: CPointer<ByteVar>?, inputType: CPointer<ByteVar>?, targetType: CPointer<ByteVar>?, targetVersion: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, inputType, targetType, targetVersion, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val path = MigrationPath(GraphSpec.Json.configuration.migrations)
        val schema = format.decodeFromString(input[0])
        var map = schema as? SchemaMap ?: error("Schema format expected")
        map = path.migrate(map, type = input[1], targetType = input[2], targetVersion = input[3])
        format.encodeToString(map)
    }

