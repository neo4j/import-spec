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
package org.neo4j.importer.v1.actions;

/**
 * {@link ActionStage} defines when the enclosing {@link Action} must run.<br>
 * {@link org.neo4j.importer.v1.pipeline.ImportPipeline} translate this into concrete dependencies.
 */
public enum ActionStage {
    /**
     * The enclosing {@link Action} must run before anything else (actively-used sources, active targets)
     */
    START,
    /**
     * The enclosing {@link Action} must run after all actively-used {@link org.neo4j.importer.v1.sources.Source}s
     */
    POST_SOURCES,
    /**
     * The enclosing {@link Action} must run before all active {@link org.neo4j.importer.v1.targets.NodeTarget}s
     */
    PRE_NODES,
    /**
     * The enclosing {@link Action} must run after all active {@link org.neo4j.importer.v1.targets.NodeTarget}s
     */
    POST_NODES,
    /**
     * The enclosing {@link Action} must run before all active {@link org.neo4j.importer.v1.targets.RelationshipTarget}s
     */
    PRE_RELATIONSHIPS,
    /**
     * The enclosing {@link Action} must run after all active {@link org.neo4j.importer.v1.targets.RelationshipTarget}s
     */
    POST_RELATIONSHIPS,
    /**
     * The enclosing {@link Action} must run before all active {@link org.neo4j.importer.v1.targets.CustomQueryTarget}s
     */
    PRE_QUERIES,
    /**
     * The enclosing {@link Action} must run after all active {@link org.neo4j.importer.v1.targets.CustomQueryTarget}s
     */
    POST_QUERIES,
    /**
     * The enclosing {@link Action} must run after everything else (actively-used sources, active targets)
     */
    END
}
