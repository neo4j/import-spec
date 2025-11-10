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

import java.io.Serializable;

/**
 * {@link Action} define one-off execution scripts.<br>
 * The {@link Action} name must be unique within an instance of {@link org.neo4j.importer.v1.ImportSpecification}.<br>
 * The {@link Action} stage (see {@link Action#getStage()} determines when an action must be executed.<br>
 * Only {@link org.neo4j.importer.v1.actions.plugin.CypherAction} is defined by default.<br>
 * Other {@link ActionProvider} implementation may be registered through Java's standard
 * Service Provider Interface mechanism.
 */
public interface Action extends Serializable {

    boolean DEFAULT_ACTIVE = true;

    /**
     * Type of the action (example: "jdbc", "http", ...)<br>
     * The type name must be unique in a case-insensitive (per {@link java.util.Locale#ROOT} casing rules).<br>
     * Having multiple actions loaded with the same type is invalid and will lead to an exception being raised, aborting
     * the import early.<br>
     * Note: it is recommended that the type returned here be the same as the one this source's {@link ActionProvider}
     * supports, or a subset thereof.
     */
    String getType();

    /**
     * Action name.
     * It must always be provided and must be globally unique.
     */
    String getName();

    /**
     * Whether the action is active or not.
     * Default value must be true.
     */
    boolean isActive();

    /**
     * Defines the import stage at which the action runs.
     * It must always be provided.
     */
    ActionStage getStage();
}
