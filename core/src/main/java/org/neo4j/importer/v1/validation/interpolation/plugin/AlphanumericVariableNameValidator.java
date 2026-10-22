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
import java.util.regex.Pattern;
import org.neo4j.importer.v1.validation.interpolation.InterpolationValidationResult;
import org.neo4j.importer.v1.validation.interpolation.InterpolationVariableValidator;

public class AlphanumericVariableNameValidator implements InterpolationVariableValidator {

    // this allows all kinds of international letters and integers as well as underscore as separator
    static final Pattern EXACT_VARIABLE_NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L}\\p{Nd}_]*$");

    @Override
    public Set<Class<? extends InterpolationVariableValidator>> requires() {
        return Set.of(NotNullVariableNameValidator.class, NonDollarPrefixedVariableNameValidator.class);
    }

    @Override
    public InterpolationValidationResult validate(String name, Object value) {
        if (!EXACT_VARIABLE_NAME_PATTERN.matcher(name).matches()) {
            return InterpolationValidationResult.builder()
                    .addError(
                            "The name of variable '%s' must start with a letter, optionally followed by _ and/or other letters/numbers",
                            name)
                    .build();
        }
        return InterpolationValidationResult.success();
    }
}
