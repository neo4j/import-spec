/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

data class SchemaMap(val content: MutableMap<String, SchemaElement>) :
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

    fun list(key: String) = listOrNull(key) ?: error("Expected list '$key'")

    fun listOrNull(key: String): SchemaList? = content[key]?.let { (it as SchemaList) }

    fun map(key: String) = mapOrNull(key) ?: error("Expected map for key '$key'")

    fun mapOrNull(key: String): SchemaMap? = content[key]?.let { (it as SchemaMap) }

    fun bool(key: String) = string(key).toBooleanStrict()

    fun string(key: String) = stringOrNull(key) ?: error("Expected key '$key'")

    fun stringOrNull(key: String) = literalOrNull(key)?.string

    fun literal(key: String) = literalOrNull(key) ?: error("Expected key '$key'")

    fun literalOrNull(key: String) = content[key]?.let { it as? SchemaLiteral }

    fun intOrNull(key: String) = stringOrNull(key)?.toIntOrNull()
}

fun schemaMapOf(vararg pairs: Pair<String, SchemaElement>) = SchemaMap(pairs.toMap().toMutableMap())
