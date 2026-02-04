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
package org.neo4j.importer.v1.validation.plugin;

import java.util.ArrayList;
import java.util.List;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.plugin.CypherAction;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoNullCypherActionExecutionModeValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "CYPH-002";

    private final List<Integer> offendingIndices = new ArrayList<>();

    @Override
    public void visitAction(int index, Action action) {
        if (!(action instanceof CypherAction)) {
            return;
        }
        CypherAction cypherAction = (CypherAction) action;
        if (cypherAction.getExecutionMode() == null) {
            offendingIndices.add(index);
        }
    }

    @Override
    public boolean report(Builder builder) {
        offendingIndices.forEach(index -> {
            var path = String.format("$.actions[%d].execution_mode", index);
            builder.addError(path, ERROR_CODE, String.format("%s must be one of: TRANSACTION, AUTOCOMMIT", path));
        });
        return !offendingIndices.isEmpty();
    }
}
