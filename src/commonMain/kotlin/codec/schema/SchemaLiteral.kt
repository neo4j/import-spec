/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

class SchemaLiteral(override val string: String) : SchemaPrimitive() {
    override val isString: Boolean
        get() = true

    override fun toString(): String = string
}
