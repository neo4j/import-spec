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
package org.neo4j.importer.v1.sources;

import java.io.Serializable;

/**
 * A {@link Source} provides a collection of key-value pairs.
 * Each {@link org.neo4j.importer.v1.targets.Target} is bound to a single {@link Source}, using the source's name.
 * The source's key-value pairs are mapped by the target's {@link org.neo4j.importer.v1.targets.PropertyMapping} in the case
 * of {@link org.neo4j.importer.v1.targets.NodeTarget} and {@link org.neo4j.importer.v1.targets.RelationshipTarget}.
 * They are otherwise arbitrarily mapped by the Cypher query defined by {@link org.neo4j.importer.v1.targets.CustomQueryTarget}.
 * A source name must be unique within an instance of {@link org.neo4j.importer.v1.ImportSpecification}.
 * A custom source can only be used if a corresponding {@link SourceProvider} is registered, through Java's standard
 * Service Provider Interface mechanism.
 */
public interface Source extends Serializable {

    /**
     * Type of the source (example: "jdbc", "text", "parquet", ...)
     * The type name must be unique in a case-insensitive (per {@link java.util.Locale#ROOT} casing rules).
     * Having multiple sources loaded with the same type is invalid and will lead to an exception being raised.
     * Note: it is recommended that the type returned here be the same as the one this source's {@link SourceProvider} supports.
     */
    String getType();

    /**
     * Name of a particular source instance
     * The name is user-provided and must be unique within the whole {@link org.neo4j.importer.v1.ImportSpecification}.
     * This name is used as a reference for {@link org.neo4j.importer.v1.targets.Target}s to declare the source of the
     * data they map from.
     */
    String getName();
}
