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
package org.neo4j.importer.v1.validation;

import java.io.Reader;
import java.util.Set;
import org.neo4j.importer.v1.Configuration;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;

/**
 * This is the SPI for custom validators.
 * Custom validators have the ability to validate elements of an {@link org.neo4j.importer.v1.ImportSpecification}.
 * The import specification at this stage is guaranteed to comply to the official import specification JSON schema.
 * Every custom validator is instantiated only once (per {@link org.neo4j.importer.v1.ImportSpecificationDeserializer#deserialize(Reader)} call.
 * The validation order is as follows:
 * 1. visitConfiguration
 * 2. visitSource (as many times as there are sources)
 * 3. visitNodeTarget (as many times as there are node targets)
 * 4. visitRelationshipTarget (as many times as there are relationship targets)
 * 5. visitCustomQueryTarget (as many times as there are custom query targets)
 * 6. visitAction (as many times as there are actions)
 * Then {@link SpecificationValidator#report(Builder)} is called with a {@link SpecificationValidationResult.Builder}, where
 * errors are reported via {@link SpecificationValidationResult.Builder#addError(String, String, String)} and warnings
 * via {@link SpecificationValidationResult.Builder#addWarning(String, String, String)}.
 * Implementations are not expected to be thread-safe.
 * Implementations must not make any assumptions about the invocation order of other implementations.
 * If an implementation depends on the successful validation of another one, please include the latter to {@link SpecificationValidator#requires()}.
 * Mutating the provided arguments via any of the visitXxx or accept calls is considered undefined behavior.
 */
public interface SpecificationValidator {

    /**
     * Reports validation errors and warnings via {@link SpecificationValidationResult.Builder}
     *
     * @return true if at least 1 error was reported, false otherwise
     */
    boolean report(SpecificationValidationResult.Builder builder);

    /**
     * Declares validators whose validation must be successful before
     * this validator can report errors and warnings.
     * In other words, if any of the validation of the returned validators do not pass,
     * this validator's implementation {@link SpecificationValidator#report(Builder)} will not be called.
     * It is however possible that this validator visit methods are called regardless.
     *
     * @return the set of validator implementations whose validation must pass before this validator reports anything
     */
    default Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of();
    }

    default void visitConfiguration(Configuration configuration) {}

    default void visitSource(int index, Source source) {}

    default void visitNodeTarget(int index, NodeTarget target) {}

    default void visitRelationshipTarget(int index, RelationshipTarget target) {}

    default void visitCustomQueryTarget(int index, CustomQueryTarget target) {}

    default void visitAction(int index, Action action) {}
}
