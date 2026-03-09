/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

class SchemaList(val content: MutableList<SchemaElement>) :
    SchemaElement, MutableList<SchemaElement> by content {
    override fun equals(other: Any?): Boolean = content == other

    override fun hashCode(): Int = content.hashCode()

    override fun toString(): String =
        content.joinToString(prefix = "[", postfix = "]", separator = ",")
}

fun schemaListOf(vararg elements: SchemaElement): SchemaList {
    return SchemaList(elements.toMutableList())
}

fun schemaListOf(vararg elements: String): SchemaList {
    return SchemaList(elements.map { SchemaLiteral(it) }.toMutableList())
}
