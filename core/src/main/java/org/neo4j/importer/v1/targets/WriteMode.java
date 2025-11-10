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
package org.neo4j.importer.v1.targets;

/**
 * {@link WriteMode} controls how an entity (node, relationship) is written to the graph.
 */
public enum WriteMode {
    /**
     * This tells the backend to insert the entity, regardless of similar entities already persisted in the graph
     * @see <a href="https://neo4j.com/docs/cypher-manual/current/clauses/create/">Cypher's CREATE clause</a>
     */
    CREATE,
    /**
     * This tells the backend to insert the entity, if no similar entity has been found in the graph (this can still
     * lead to duplicates unless a uniqueness constraint has been set for the pattern being merged)
     * @see <a href="https://neo4j.com/docs/cypher-manual/current/clauses/merge/">Cypher's MERGE clause</a>
     */
    MERGE
}
