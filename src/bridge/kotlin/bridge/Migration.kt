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
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import migrate.migration.dataModel.DataModelV2V3Migration
import migrate.migration.dataModel.DataModelV3GraphSpecMigration
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("migrate_v3_to_graph_spec")
fun MigrateV3ToGraphSpec(inputJson: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val format = JsonFormat.build()
        val schema = format.decodeFromString(input[0]) as SchemaMap
        val migrated = DataModelV3GraphSpecMigration().migrate(schema)
        format.encodeToString(migrated)
    }

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("migrate_v2_to_v3")
fun MigrateV2ToV3(inputJson: CPointer<ByteVar>?, inputVersion: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, inputVersion, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val format = JsonFormat.build()
        val schema = format.decodeFromString(input[0]) as SchemaMap
        val migrated = DataModelV2V3Migration(input[1]).migrate(schema)
        format.encodeToString(migrated)
    }
