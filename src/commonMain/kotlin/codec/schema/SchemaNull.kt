/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

object SchemaNull : SchemaPrimitive() {
    override val isString: Boolean
        get() = false

    override val string: String = "null"
}
