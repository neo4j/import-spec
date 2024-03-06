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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SpecificationValidationResult {

    private final Set<SpecificationError> errors;
    private final Set<SpecificationWarning> warnings;

    // visible for testing
    SpecificationValidationResult(Set<SpecificationError> errors, Set<SpecificationWarning> warnings) {
        this.errors = errors;
        this.warnings = warnings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean passes() {
        return errors.isEmpty();
    }

    public Set<SpecificationError> getErrors() {
        return errors;
    }

    public Set<SpecificationWarning> getWarnings() {
        return warnings;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SpecificationValidationResult that = (SpecificationValidationResult) object;
        return Objects.equals(errors, that.errors) && Objects.equals(warnings, that.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errors, warnings);
    }

    public static final class Builder {

        private final Set<SpecificationError> errors = new LinkedHashSet<>();
        private final Set<SpecificationWarning> warnings = new LinkedHashSet<>();

        public Builder addError(String elementPath, String code, String message) {
            return addError(new SpecificationError(elementPath, code, message));
        }

        public Builder addWarning(String elementPath, String code, String message) {
            return addWarning(new SpecificationWarning(elementPath, code, message));
        }

        public SpecificationValidationResult build() {
            return new SpecificationValidationResult(errors, warnings);
        }

        private Builder addError(SpecificationError error) {
            errors.add(error);
            return this;
        }

        private Builder addWarning(SpecificationWarning warning) {
            warnings.add(warning);
            return this;
        }
    }
}
