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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InvalidSpecificationExceptionTest {

    @Test
    void formats_validation_with_errors_as_message() {
        var result = new SpecificationValidationResult(
                newLinkedHashSet(
                        new SpecificationError("$.sources", "OOPSIE", "something wrong occurred"),
                        new SpecificationError("$.targets", "WELP", "something bad happened")),
                newLinkedHashSet());

        var exception = new InvalidSpecificationException(result);

        assertThat(exception)
                .hasMessageContaining(
                        """
                Import specification is invalid, see report below:
                ===============================================================================
                Summary
                ===============================================================================
                \t- 2 error(s)
                \t- 0 warning(s)

                === Errors ===
                \t- [OOPSIE][$.sources] something wrong occurred
                \t- [WELP][$.targets] something bad happened
                ===============================================================================\
                """
                                .stripIndent());
    }

    @Test
    void formats_validation_with_errors_and_warnings_as_message() {
        var result = new SpecificationValidationResult(
                newLinkedHashSet(
                        new SpecificationError("$.sources", "OOPSIE", "something wrong occurred"),
                        new SpecificationError("$.targets", "WELP", "something bad happened")),
                newLinkedHashSet(
                        new SpecificationWarning("$.actions[0].name", "HMMM", "something is a bit strange"),
                        new SpecificationWarning("$.targets[2].depends-on", "AHEM", "are you sure this is right")));

        var exception = new InvalidSpecificationException(result);

        assertThat(exception)
                .hasMessageContaining(
                        """
                Import specification is invalid, see report below:
                ===============================================================================
                Summary
                ===============================================================================
                \t- 2 error(s)
                \t- 2 warning(s)

                === Errors ===
                \t- [OOPSIE][$.sources] something wrong occurred
                \t- [WELP][$.targets] something bad happened

                === Warnings ===
                \t- [HMMM][$.actions[0].name] something is a bit strange
                \t- [AHEM][$.targets[2].depends-on] are you sure this is right
                ===============================================================================\
                """
                                .stripIndent());
    }

    @SafeVarargs
    private static <T> Set<T> newLinkedHashSet(T... items) {
        return new LinkedHashSet<>(Arrays.asList(items));
    }
}
