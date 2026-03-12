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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import model.GraphModel
import model.mapping.Mapping
import model.mapping.NodeMapping
import model.mapping.QueryMapping
import model.mapping.RelationshipMapping
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlBuilder
import net.mamoe.yamlkt.YamlElement
import net.mamoe.yamlkt.YamlList
import net.mamoe.yamlkt.YamlLiteral
import net.mamoe.yamlkt.YamlMap
import net.mamoe.yamlkt.YamlNull
import net.mamoe.yamlkt.YamlPrimitive
import kotlin.collections.component1
import kotlin.collections.component2

class YamlFormat(private val yaml: Yaml, private val json: JsonFormat) : Format {
    override fun encodeToString(element: SchemaElement) = yaml.encodeToString(element.toYaml())

    override fun decodeFromString(string: String) = schemaElement(yaml.decodeYamlFromString(string))

    override fun encodeToSchema(model: GraphModel) = json.encodeToSchema(model)

    /** [Yaml] library doesn't have proper AST -> Generic type support so we fall back to [json] */
    override fun decodeFromSchema(element: SchemaElement) = json.decodeFromSchema(element)

    fun schemaElement(yaml: YamlElement, parent: String = ""): SchemaElement = when (yaml) {
        is YamlList -> SchemaList(
            yaml.mapIndexed { index, element -> schemaElement(element, "$parent[$index]") }
                .toMutableList()
        )
        is YamlMap -> {
            val content =
                yaml.content
                    .map { (key, value) ->
                        if (key !is YamlLiteral) {
                            error("Failed to parse yaml: non-string key not supported: $key")
                        }
                        val element =
                            schemaElement(value, if (parent == "") key.content else "$parent.${key.content}")
                        Pair(key.content, element)
                    }
                    .toMap()
                    .toMutableMap()
            SchemaMap(content)
        }
        is YamlLiteral -> SchemaLiteral(yaml.content, parent)
        YamlNull -> SchemaNull
    }

    fun SchemaElement.toYaml(): YamlElement = when (this) {
        is SchemaList -> YamlList(content.map { it.toYaml() })
        is SchemaMap -> YamlMap(content.mapValues { (_, value) -> value.toYaml() })
        is SchemaLiteral -> YamlPrimitive(string)
        SchemaNull -> YamlNull
    }

    companion object Builder : Format.Builder {
        override fun build() = YamlFormat(
            Yaml {
                encodeDefaultValues = false
            },
            JsonFormat.build()
        )
    }
}
