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
package org.neo4j.importer.v1.validation.interpolation.plugin;

import java.util.Set;
import org.neo4j.importer.v1.validation.interpolation.InterpolationValidationResult;
import org.neo4j.importer.v1.validation.interpolation.InterpolationVariableValidator;

public class PrimitiveVariableValueValidator implements InterpolationVariableValidator {

    @Override
    public Set<Class<? extends InterpolationVariableValidator>> requires() {
        return Set.of(NotNullVariableValueValidator.class);
    }

    @Override
    public InterpolationValidationResult validate(String name, Object value) {
        if (!(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
            return InterpolationValidationResult.builder()
                    .addError(
                            "The value of variable '%s' must be a string, number or boolean, found: %s",
                            name, value.getClass())
                    .build();
        }
        return InterpolationValidationResult.success();
    }
}
