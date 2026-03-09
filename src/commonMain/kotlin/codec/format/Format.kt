/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.format

import codec.schema.SchemaElement
import model.GraphModel

interface Format {
    fun encodeToString(element: SchemaElement): String

    fun decodeFromString(string: String): SchemaElement

    fun encodeToSchema(model: GraphModel): SchemaElement

    fun decodeFromSchema(element: SchemaElement): GraphModel

    interface Builder {
        fun build(): Format
    }
}
