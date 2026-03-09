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

    fun string(key: String) = strOrNull(key) ?: error("Expected key '$key'")

    fun strOrNull(key: String) = content[key]?.let { (it as? SchemaLiteral)?.string }

    fun intOrNull(key: String) = strOrNull(key)?.toIntOrNull()
}
