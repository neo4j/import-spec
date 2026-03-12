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
import codec.format.Format
import codec.format.JsonFormat
import codec.format.Prettify
import codec.format.YamlFormat
import codec.schema.SchemaMap
import migrate.MigrationPath
import migrate.migration.dataModel.DataModelV2V3Migration
import model.GraphModel
import model.Type
import model.Version
import kotlin.js.JsExport

@JsExport
sealed class GraphSpec(val configuration: GraphSpecConfig, val builder: Format.Builder) {
    private val path = MigrationPath(configuration.migrations)
    private val format = builder.build()

    fun encodeToString(model: GraphModel, targetVersion: String = Version.LATEST, pretty: Boolean = true): String {
        val schema = format.encodeToSchema(model)
        println("Encoded $schema")
        var map = schema as? SchemaMap ?: error("Schema format expected")
        map = path.migrate(map, Type.GRAPH_SPEC, targetVersion)
        if (targetVersion == Version.LATEST && pretty) {
            map = Prettify.transform(map)
        }
        return format.encodeToString(map)
    }

    fun decodeFromString(content: String, type: String = Type.GRAPH_SPEC): GraphModel {
        val schema = format.decodeFromString(content)
        var map = schema as? SchemaMap ?: error("Schema format expected")
        map = path.migrate(map, type, Version.LATEST)
        return format.decodeFromSchema(map)
    }

    object Json : GraphSpec(defaultConfig(), JsonFormat.Builder)

    object Yaml : GraphSpec(defaultConfig(), YamlFormat.Builder)
}

private fun defaultConfig(): GraphSpecConfig {
    val builder = GraphSpecConfig.Builder()
    builder.migrate(DataModelV2V3Migration(Version.DATA_MODEL_V23))
    builder.migrate(DataModelV2V3Migration(Version.DATA_MODEL_V24))
    return builder.build()
}

private class GraphSpecImpl(configuration: GraphSpecConfig, format: Format.Builder) :
    GraphSpec(configuration, format)

fun GraphSpec(from: GraphSpec = GraphSpec.Json, builderAction: GraphSpecConfig.Builder.() -> Unit): GraphSpec {
    val builder = GraphSpecConfig.Builder(from.configuration)
    builder.builderAction()
    val conf = builder.build()
    return GraphSpecImpl(conf, from.builder)
}
