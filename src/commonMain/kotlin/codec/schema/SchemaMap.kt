/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

data class SchemaMap(val content: MutableMap<String, SchemaElement>, val path: String = "") :
    SchemaElement, MutableMap<String, SchemaElement> by content {
    override fun equals(other: Any?): Boolean = content == other

    override fun hashCode(): Int = content.hashCode()

    override fun toString(): String =
        content.entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) ->
                buildString {
                    append('"').append(k).append('"')
                    append(':')
                    append(v)
                }
            },
        )

    operator fun set(key: String, value: String) {
        content[key] = SchemaLiteral(value)
    }

    operator fun set(key: String, value: SchemaElement) {
        content[key] = value
    }

    fun list(key: String): SchemaList {
        val list = content[key] ?: error("Missing required list at $path.$key")
        return list as? SchemaList ?: error("Expected list, found invalid type ${list::class.simpleName} at $path.$key")
    }

    fun listOrNull(key: String): SchemaList? = content[key]?.let { it as? SchemaList }

    fun map(key: String): SchemaMap {
        val map = content[key] ?: error("Missing required map at $path.$key")
        return map as? SchemaMap ?: error("Expected map, found invalid type ${map::class.simpleName} at $path.$key")
    }

    fun mapOrNull(key: String): SchemaMap? = content[key]?.let { it as? SchemaMap }

    fun literal(key: String): SchemaLiteral {
        val literal = content[key] ?: error("Missing required literal at $path.$key")
        return literal as? SchemaLiteral
            ?: error("Expected literal, found invalid type ${literal::class.simpleName} at $path.$key")
    }

    fun literalOrNull(key: String) = content[key]?.let { it as? SchemaLiteral }

    fun bool(key: String) = string(key).toBooleanStrict()

    fun boolOrNull(key: String) = string(key).toBooleanStrictOrNull()

    fun string(key: String) = literal(key).string

    fun stringOrNull(key: String) = literalOrNull(key)?.string

    fun int(key: String) = stringOrNull(key)?.toInt()

    fun intOrNull(key: String) = stringOrNull(key)?.toIntOrNull()

    fun mapOfMaps(key: String): Map<String, SchemaMap> {
        val map = content[key] ?: error("Missing required map at $path.$key")
        val schemaMap =
            map as? SchemaMap ?: error("Expected map, found invalid type ${map::class.simpleName} at $path.$key")
        return schemaMap.content.map { (key, element) ->
            val child = element as? SchemaMap
                ?: error("Expected map, found invalid type ${map::class.simpleName} at $path.$key")
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
        val list = content[key] ?: error("Missing required list at $path.$key")
        val schemaList =
            list as? SchemaList ?: error("Expected list, found invalid type ${list::class.simpleName} at $path.$key")
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
}

fun schemaMapOf(vararg pairs: Pair<String, SchemaElement>) = SchemaMap(pairs.toMap().toMutableMap())
