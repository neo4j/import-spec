/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.format

import codec.schema.SchemaElement
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.SchemaNull
import model.GraphModel
import kotlin.collections.component1
import kotlin.collections.component2
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlBuilder
import net.mamoe.yamlkt.YamlElement
import net.mamoe.yamlkt.YamlList
import net.mamoe.yamlkt.YamlLiteral
import net.mamoe.yamlkt.YamlMap
import net.mamoe.yamlkt.YamlNull
import net.mamoe.yamlkt.YamlPrimitive

class YamlFormat(private val yaml: Yaml, private val json: JsonFormat) : Format {
    override fun encodeToString(element: SchemaElement) = yaml.encodeToString(element.toYaml())

    override fun decodeFromString(string: String) = schemaElement(yaml.decodeYamlFromString(string))

    override fun encodeToSchema(model: GraphModel) = json.encodeToSchema(model)

    /** [Yaml] library doesn't have proper AST -> Generic type support so we fall back to [json] */
    override fun decodeFromSchema(element: SchemaElement) = json.decodeFromSchema(element)

    fun schemaElement(yaml: YamlElement): SchemaElement =
        when (yaml) {
            is YamlList -> SchemaList(yaml.map { schemaElement(it) }.toMutableList())
            is YamlMap -> {
                val content =
                    yaml.content
                        .map { (key, value) ->
                            if (key !is YamlLiteral) {
                                error("Failed to parse yaml: non-string key not supported: $key")
                            }
                            Pair(key.content, schemaElement(value))
                        }
                        .toMap()
                        .toMutableMap()
                SchemaMap(content)
            }
            is YamlLiteral -> SchemaLiteral(yaml.content)
            YamlNull -> SchemaNull
        }

    fun SchemaElement.toYaml(): YamlElement =
        when (this) {
            is SchemaList -> YamlList(content.map { it.toYaml() })
            is SchemaMap -> YamlMap(content.mapValues { (_, value) -> value.toYaml() })
            is SchemaLiteral -> YamlPrimitive(string)
            SchemaNull -> YamlNull
        }

    companion object Builder : Format.Builder {
        override fun build() =
            YamlFormat(
                Yaml {
                    encodeDefaultValues = false
                    listSerialization = YamlBuilder.ListSerialization.FLOW_SEQUENCE
                },
                JsonFormat.build(),
            )
    }
}
