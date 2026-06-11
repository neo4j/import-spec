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
package org.neo4j.importer.v1.validation.plugin.graphtype;

import java.util.concurrent.atomic.AtomicBoolean;
import org.neo4j.importer.v1.Configuration;
import org.neo4j.importer.v1.config.Features;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoCustomQueryTargetValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "GTNQ-001";

    private final AtomicBoolean invalid = new AtomicBoolean(false);

    private final AtomicBoolean skipValidation = new AtomicBoolean(true);

    @Override
    public void visitConfiguration(Configuration configuration) {
        skipValidation.set(!Features.isGraphTypeEnabled(configuration));
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        if (skipValidation.get()) {
            return;
        }
        invalid.set(true);
    }

    @Override
    public boolean report(Builder builder) {
        if (invalid.get()) {
            builder.addError(
                    "$.targets.queries",
                    ERROR_CODE,
                    "Graph type cannot be generated: custom query targets are not supported");
        }
        return false;
    }
}
