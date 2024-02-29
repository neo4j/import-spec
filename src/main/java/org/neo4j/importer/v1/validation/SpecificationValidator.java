/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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

import java.util.Map;
import java.util.function.Consumer;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;

public interface SpecificationValidator extends Consumer<SpecificationValidationResult.Builder> {

    default void visitConfiguration(Map<String, Object> configuration) {}

    default void visitSource(int index, Source source) {}

    default void visitNodeTarget(int index, NodeTarget target) {}

    default void visitRelationshipTarget(int index, RelationshipTarget target) {}

    default void visitCustomQueryTarget(int index, CustomQueryTarget target) {}

    default void visitAction(int index, Action action) {}
}
