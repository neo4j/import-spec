/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

sealed class SchemaPrimitive : SchemaElement {
    abstract val isString: Boolean
    abstract val string: String
}
