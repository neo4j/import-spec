/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 * This file is part of Neo4j internal tooling.
 */
package codec.schema

/**
 * A format agnostic AST used for transforms and migrations
 *
 * @see [codec.format.Format] for conversions
 * @see [migrate.Migration] for usages
 */
sealed interface SchemaElement
