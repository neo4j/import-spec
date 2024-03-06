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

import java.util.Set;

public class InvalidSpecificationException extends SpecificationException {
    public InvalidSpecificationException(SpecificationValidationResult result) {
        super(String.format("Import specification is invalid, see report below:\n%s", validationReport(result)));
    }

    // TODO: wrap long error messages
    private static String validationReport(SpecificationValidationResult result) {
        Set<SpecificationError> errors = result.getErrors();
        Set<SpecificationWarning> warnings = result.getWarnings();
        return String.format(
                "===============================================================================\n" + "Summary\n"
                        + "===============================================================================\n"
                        + "\t- %d error(s)\n"
                        + "\t- %d warning(s)\n\n"
                        + "%s"
                        + "%s"
                        + "===============================================================================",
                errors.size(), warnings.size(), errorReport(errors), warningReport(warnings));
    }

    private static String errorReport(Set<SpecificationError> errors) {
        if (errors.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("=== Errors ===\n");
        errors.forEach((error) -> {
            builder.append(
                    String.format("\t- [%s][%s] %s\n", error.getCode(), error.getElementPath(), error.getMessage()));
        });
        return builder.toString();
    }

    private static String warningReport(Set<SpecificationWarning> warnings) {
        if (warnings.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("\n=== Warnings ===\n");
        warnings.forEach((warning) -> {
            builder.append(String.format(
                    "\t- [%s][%s] %s\n", warning.getCode(), warning.getElementPath(), warning.getMessage()));
        });
        return builder.toString();
    }
}
