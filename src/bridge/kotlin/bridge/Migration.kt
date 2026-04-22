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
import migrate.MigrationPath
import model.Type
import model.Version
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("migrate_to_graph_spec")
fun migrateToGraphSpec(inputJson: CPointer<ByteVar>?, inputType: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, inputType, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val path = MigrationPath(GraphSpec.Json.configuration.migrations)
        val format = JsonFormat.build()
        val schema = format.decodeFromString(input[0])
        var map = schema as? SchemaMap ?: error("Schema format expected")
        map = path.migrate(map, input[1], Version.LATEST, Type.GRAPH_SPEC)
        format.encodeToString(map)
    }

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("migrate_from_graph_spec")
fun migrateFromGraphSpec(inputJson: CPointer<ByteVar>?, targetType: CPointer<ByteVar>?, targetVersion: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, targetType, targetVersion, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val graphModel = GraphSpec.Json.decodeFromString(content = input[0])
        GraphSpec.Json.encodeToString(graphModel, targetType = input[1], targetVersion = input[2])
    }
