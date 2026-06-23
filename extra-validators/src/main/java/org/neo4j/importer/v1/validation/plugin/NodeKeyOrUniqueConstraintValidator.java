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

import java.util.LinkedHashMap;
import java.util.Map;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.WriteMode;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NodeKeyOrUniqueConstraintValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "NKUC-001";

    private final Map<String, String> keylessMergeTargets = new LinkedHashMap<>();

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        if (!target.isActive() || target.getWriteMode() != WriteMode.MERGE) {
            return;
        }
        var schema = target.getSchema();
        if (schema.getKeyConstraints().isEmpty()
                && schema.getUniqueConstraints().isEmpty()) {
            keylessMergeTargets.put(
                    String.format("$.targets.nodes[%d].schema", index),
                    String.format(
                            "Node target '%s' with write_mode merge requires at least one node key constraint or unique constraint, none found",
                            target.getName()));
        }
    }

    @Override
    public boolean report(SpecificationValidationResult.Builder builder) {
        keylessMergeTargets.forEach((path, message) -> builder.addError(path, ERROR_CODE, message));
        return !keylessMergeTargets.isEmpty();
    }
}
