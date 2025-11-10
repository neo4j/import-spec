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
 * {@link NodeMatchMode} controls how relationship start/end nodes are looked up.
 */
public enum NodeMatchMode {
    /**
     * This tells the backend to match the start/end node, or skip the relationship if the corresponding node is not
     * found in the graph
     * @see <a href="https://neo4j.com/docs/cypher-manual/current/clauses/match/">Cypher's MATCH clause</a>
     */
    MATCH,
    /**
     * This tells the backend to find or create the start/end node
     * @see <a href="https://neo4j.com/docs/cypher-manual/current/clauses/merge/">Cypher's MERGE clause</a>
     */
    MERGE
}
