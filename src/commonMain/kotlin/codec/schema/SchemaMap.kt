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
package codec.schema

import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

data class SchemaMap(val content: MutableMap<String, SchemaElement> = mutableMapOf(), override val path: String = "") :
    SchemaElement,
    Map<String, SchemaElement> by content {
    override fun equals(other: Any?): Boolean = content == other

    override fun repath(newPath: String) = SchemaMap(
        content.map { (key, element) -> key to element.repath("$newPath.$key") }.toMap().toMutableMap(),
        newPath
    )

    constructor(content: MutableMap<String, out SchemaElement>) : this(content as MutableMap<String, SchemaElement>)

    override fun hashCode(): Int = content.hashCode()

    override fun toString(): String = content.entries.joinToString(
        separator = ",",
        prefix = "{",
        postfix = "}",
        transform = { (k, v) ->
            buildString {
                append('"').append(k).append('"')
                append(':')
                append(v)
            }
        }
    )

    operator fun set(key: String, value: Boolean) {
        content[key] = SchemaLiteral(value.toString(), path(key))
    }

    operator fun set(key: String, value: String) {
        content[key] = SchemaLiteral(value, path(key))
    }

    operator fun set(key: String, value: Map<String, SchemaElement>) {
        put(key, SchemaMap(value.toMutableMap()))
    }

    operator fun set(key: String, value: List<SchemaElement>) {
        put(key, SchemaList(value.toMutableList()))
    }

    operator fun set(key: String, value: SchemaElement) {
        put(key, value)
    }

    operator fun set(key: String, value: SchemaMap) {
        put(key, value)
    }

    operator fun set(key: String, value: SchemaList) {
        put(key, value)
    }

    operator fun set(key: String, value: SchemaLiteral) {
        put(key, value)
    }

    fun remove(key: String) = content.remove(key)

    fun removeMap(key: String): SchemaMap {
        val map = content.remove(key) ?: error("Expected map, found nothing at ${path(key)}")
        return map as? SchemaMap ?: error("Expected map, found invalid type ${map::class.simpleName} at ${path(key)}")
    }

    fun put(key: String, value: SchemaElement): SchemaElement? = content.put(key, value.repath(path(key)))

    fun putAll(from: Map<out String, SchemaElement>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    fun list(key: String): SchemaList {
        val list = content[key] ?: error("Missing required list at ${path(key)}")
        return list as? SchemaList
            ?: error("Expected list, found invalid type ${list::class.simpleName} at ${path(key)}")
    }

    fun listOrNull(key: String): SchemaList? = content[key]?.let { it as? SchemaList }

    fun map(key: String): SchemaMap {
        val map = content[key] ?: error("Missing required map at ${path(key)}")
        return map as? SchemaMap ?: error("Expected map, found invalid type ${map::class.simpleName} at ${path(key)}")
    }

    fun mapOrNull(key: String): SchemaMap? = content[key]?.let { it as? SchemaMap }

    fun mapOrPut(key: String): SchemaMap = content.getOrPut(key) { SchemaMap(path = path(key)) } as SchemaMap

    fun literal(key: String): SchemaLiteral {
        val literal = content[key] ?: error("Missing required literal at ${path(key)}")
        return literal as? SchemaLiteral
            ?: error("Expected literal, found invalid type ${literal::class.simpleName} at ${path(key)}")
    }

    fun literalOrNull(key: String) = content[key]?.let { it as? SchemaLiteral }

    fun bool(key: String) = string(key).toBooleanStrict()

    fun boolOrNull(key: String) = string(key).toBooleanStrictOrNull()

    fun string(key: String) = literal(key).string

    fun stringOrNull(key: String) = literalOrNull(key)?.string

    fun int(key: String) = stringOrNull(key)?.toInt()

    fun intOrNull(key: String) = stringOrNull(key)?.toIntOrNull()

    fun mapOfMaps(key: String): Map<String, SchemaMap> {
        val map = content[key] ?: error("Missing required map at ${path(key)}")
        val schemaMap =
            map as? SchemaMap ?: error("Expected map, found invalid type ${map::class.simpleName} at ${path(key)}")
        return schemaMap.content.map { (key, element) ->
            val child = element as? SchemaMap
                ?: error("Expected map, found invalid type ${map::class.simpleName} at ${path(key)}")
            key to child
        }.toMap()
    }

    fun mapOfMapsOrNull(key: String): Map<String, SchemaMap>? {
        val map = mutableMapOf<String, SchemaMap>()
        for ((key, element) in mapOrNull(key)?.content ?: return null) {
            map[key] = element as? SchemaMap ?: return null
        }
        return map
    }

    fun listOfMaps(key: String): List<SchemaMap> {
        val list = content[key] ?: error("Missing required list at ${path(key)}")
        val schemaList =
            list as? SchemaList ?: error("Expected list, found invalid type ${list::class.simpleName} at ${path(key)}")
        return schemaList.content.mapIndexed { index, element ->
            element as? SchemaMap
                ?: error("Expected map, found invalid type ${list::class.simpleName} at $path[$index].$key")
        }
    }

    fun listOfMapsOrNull(key: String): List<SchemaMap>? {
        val list = mutableListOf<SchemaMap>()
        for (element in listOrNull(key)?.content ?: return null) {
            list.add(element as? SchemaMap ?: return null)
        }
        return list
    }

    private fun path(key: String): String = if (path.isEmpty()) key else "$path.$key"
}

fun schemaMapOf(vararg pairs: Pair<String, Any?>) = SchemaMap(
    pairs
        .filter { it.second != null }
        .associate { it.first to it.second!!.toSchemaElement() }.toMutableMap()
)

infix fun <A> A.toNotEmpty(that: Map<String, Any>?): Pair<A, Map<String, Any>?> = Pair(
    this,
    that?.takeIf {
        it.isNotEmpty()
    }
)

infix fun <A> A.toNotEmpty(that: Collection<Any>?): Pair<A, Collection<Any>?> = Pair(
    this,
    that?.takeIf {
        it.isNotEmpty()
    }
)

fun buildSchemaMap(block: MutableMap<String, Any?>.() -> Unit): SchemaMap {
    val map = mutableMapOf<String, Any?>()
    map.block()

    val schemaData = map
        .filterValues { it != null }
        .entries
        .associate { (key, value) ->
            key to value.toSchemaElement().repath(key)
        }
        .toMutableMap()
    return SchemaMap(schemaData.toMutableMap())
}
