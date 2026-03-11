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
package codec.format

import codec.schema.SchemaElement
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.SchemaNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import model.GraphModel

class JsonFormat(private val json: Json) : Format {
    override fun encodeToString(element: SchemaElement) = json.encodeToString(element.toJson())

    override fun decodeFromString(string: String) = schemaElement(json.parseToJsonElement(string))

    override fun encodeToSchema(model: GraphModel) = schemaElement(json.encodeToJsonElement(model))

    override fun decodeFromSchema(element: SchemaElement) = json.decodeFromJsonElement<GraphModel>(element.toJson())

    private fun schemaElement(json: JsonElement, parent: String = ""): SchemaElement = when (json) {
        is JsonArray -> SchemaList(
            json.mapIndexed { index, element -> schemaElement(element, "$parent[$index]") }
                .toMutableList(),
            parent
        )
        is JsonObject -> SchemaMap(
            json.mapValues { (key, value) ->
                schemaElement(
                    value,
                    if (parent == "") key else "$parent.$key"
                )
            }.toMutableMap(),
            parent
        )
        is JsonPrimitive -> json.contentOrNull?.let { SchemaLiteral(it, parent) } ?: SchemaNull
        JsonNull -> SchemaNull
    }

    private fun SchemaElement.toJson(): JsonElement = when (this) {
        is SchemaList -> JsonArray(content.map { it.toJson() })
        is SchemaMap -> JsonObject(content.mapValues { (_, value) -> value.toJson() })
        is SchemaLiteral -> JsonPrimitive(string)
        SchemaNull -> JsonNull
    }

    companion object Builder : Format.Builder {
        override fun build() = JsonFormat(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
}
