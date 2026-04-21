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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import migrate.Migration
import validate.Validation
import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
class GraphSpecConfig(
    val validators: List<Validation>,
    val migrations: Map<String, List<Migration>>,
    val format: Format.Builder,
) {
    class Builder(var format: Format.Builder) {
        val validators = mutableListOf<Validation>()
        val migrations = mutableMapOf<String, MutableList<Migration>>()

        @JsName("default")
        constructor(config: GraphSpecConfig) : this(config.format) {
            validators.addAll(config.validators)
            migrations.putAll(config.migrations.mapValues { it.value.toMutableList() })
        }

        fun json(builder: JsonBuilder.() -> Unit) {
            val json = Json {
                builder.invoke(this)
                ignoreUnknownKeys = true
                isLenient = true
            }
            this.format = object : Format.Builder {
                override fun build() = JsonFormat(json)
            }
        }

        fun format(format: Format.Builder) {
            this.format = format
        }

        fun validate(vararg validation: Validation) {
            validators.addAll(validation)
        }

        fun migrate(migration: Migration) {
            migrations.getOrPut(migration.fromKey) { mutableListOf() }.add(migration)
        }

        fun build(): GraphSpecConfig = GraphSpecConfig(
            validators = validators,
            migrations = migrations,
            format = format ?: error("No format provided."),
        )
    }
}
