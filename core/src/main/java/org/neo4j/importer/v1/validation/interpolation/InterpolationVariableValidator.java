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
package org.neo4j.importer.v1.validation.interpolation;

import java.util.Set;

public interface InterpolationVariableValidator {

    /**
     * Validate the specified interpolation variable
     * @param name variable name
     * @param value variable value
     * @return validation result
     */
    InterpolationValidationResult validate(String name, Object value);

    /**
     * Declare validators whose validation must be successful before
     * this validator can be run.
     * Validators are evaluated one at a time, in an order where every required validator is fully evaluated
     * before the validators that require it.
     * If any of the returned validators fails, this validator is skipped <strong>entirely</strong>.
     * <br>
     * As a consequence, whenever this validator's methods are invoked, all of its required validators are guaranteed to
     * have passed. Implementations may therefore rely on the guarantees enforced by their required validators and skip
     * defensively re-checking them.
     *
     * @return the set of validator implementation types whose validation must pass before this validator is run
     */
    default Set<Class<? extends InterpolationVariableValidator>> requires() {
        return Set.of();
    }
}
