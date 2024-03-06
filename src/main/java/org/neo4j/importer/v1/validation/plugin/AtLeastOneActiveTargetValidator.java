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
package org.neo4j.importer.v1.validation.plugin;

import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class AtLeastOneActiveTargetValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "IDLE-001";
    private boolean activeTargetFound = false;

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        if (target.isActive()) {
            activeTargetFound = true;
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        if (target.isActive()) {
            activeTargetFound = true;
        }
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        if (target.isActive()) {
            activeTargetFound = true;
        }
    }

    @Override
    public boolean report(Builder builder) {
        if (!activeTargetFound) {
            builder.addError("$.targets", ERROR_CODE, "at least one target must be active, none found");
            return true;
        }
        return false;
    }
}
