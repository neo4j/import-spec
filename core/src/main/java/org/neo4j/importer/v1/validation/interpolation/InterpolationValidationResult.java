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
package org.neo4j.importer.v1.validation.interpolation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class InterpolationValidationResult {

    private final Set<String> errors;

    // visible for testing
    InterpolationValidationResult(Set<String> errors) {
        this.errors = errors;
    }

    public static InterpolationValidationResult success() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean passes() {
        return errors.isEmpty();
    }

    public Set<String> getErrors() {
        return errors;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        InterpolationValidationResult that = (InterpolationValidationResult) object;
        return Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errors);
    }

    @Override
    public String toString() {
        return "InterpolationValidationResult{" + "errors=" + errors + '}';
    }

    public static final class Builder {

        private final Set<String> errors = new LinkedHashSet<>();

        public Builder addError(String message, Object... args) {
            errors.add(String.format(message, args));
            return this;
        }

        public InterpolationValidationResult build() {
            return new InterpolationValidationResult(errors);
        }
    }
}
