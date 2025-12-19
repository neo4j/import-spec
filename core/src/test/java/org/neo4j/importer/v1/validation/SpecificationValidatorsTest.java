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
package org.neo4j.importer.v1.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.sources.BigQuerySource;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.Targets;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;

class SpecificationValidatorsTest {

    @Test
    void sorts_validators_topologically() {
        // (D)-[:DEPENDS_ON]->(B)-[:DEPENDS_ON]->(A)-[:DEPENDS_ON]->(C)
        var validatorA = new A();
        var validatorB = new B();
        var validatorC = new C();
        var validatorD = new D();
        var wrapper = SpecificationValidators.of(List.of(validatorA, validatorB, validatorC, validatorD));

        List<SpecificationValidator> validators = wrapper.getValidators();

        assertThat(validators).containsExactly(validatorC, validatorA, validatorB, validatorD);
    }

    @Test
    void fails_to_sort_validators_topologically_if_there_is_a_cycle() {
        // (E)-[:DEPENDS_ON]->(F), (F)-[:DEPENDS_ON]->(E)
        var validatorE = new E();
        var validatorF = new F();

        assertThatThrownBy(() -> SpecificationValidators.of(List.of(validatorE, validatorF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The provided graph")
                .hasMessageEndingWith("defines cycles");
    }

    @Test
    void reports_validation_failure() {
        var validatorC = new C(true);

        var wrapper = SpecificationValidators.of(validatorC);

        assertThatThrownBy(() -> wrapper.validate(aSpec()))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessage("Import specification is invalid, see report below:\n"
                        + "===============================================================================\n"
                        + "Summary\n"
                        + "===============================================================================\n"
                        + "\t- 1 error(s)\n"
                        + "\t- 0 warning(s)\n"
                        + "\n"
                        + "=== Errors ===\n"
                        + "\t- [C/code][C/path] C/error message\n"
                        + "===============================================================================");
    }

    @Test
    void reports_validation_failure_of_direct_dependency_and_not_failing_dependent() {
        // both validators fail, but A requires the successful validation of C
        // since C does not validate, A's report is ignored, per
        // org.neo4j.importer.v1.validation.SpecificationValidator#requires contract
        var validatorA = new A(true);
        var validatorC = new C(true);

        var wrapper = SpecificationValidators.of(List.of(validatorA, validatorC));

        assertThatThrownBy(() -> wrapper.validate(aSpec()))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessage("Import specification is invalid, see report below:\n"
                        + "===============================================================================\n"
                        + "Summary\n"
                        + "===============================================================================\n"
                        + "\t- 1 error(s)\n"
                        + "\t- 0 warning(s)\n"
                        + "\n"
                        + "=== Errors ===\n"
                        + "\t- [C/code][C/path] C/error message\n"
                        + "===============================================================================");
    }

    @Test
    void reports_validation_failure_of_transitive_dependency_and_not_failing_dependent() {
        // B and C validators fail,
        //  - but B requires the successful validation of A (A passes, so that's OK)
        //  - and A requires the successful validation of C
        // since C does not validate, A's report is ignored and therefore B's report is ignored too, per
        // org.neo4j.importer.v1.validation.SpecificationValidator#requires contract
        var validatorA = new A(false);
        var validatorB = new B(true);
        var validatorC = new C(true);

        var wrapper = SpecificationValidators.of(List.of(validatorA, validatorC, validatorB));

        assertThatThrownBy(() -> wrapper.validate(aSpec()))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessage("Import specification is invalid, see report below:\n"
                        + "===============================================================================\n"
                        + "Summary\n"
                        + "===============================================================================\n"
                        + "\t- 1 error(s)\n"
                        + "\t- 0 warning(s)\n"
                        + "\n"
                        + "=== Errors ===\n"
                        + "\t- [C/code][C/path] C/error message\n"
                        + "===============================================================================");
    }

    static class A implements SpecificationValidator {

        private final boolean reportsError;

        public A() {
            this(false);
        }

        public A(boolean reportsError) {
            this.reportsError = reportsError;
        }

        @Override
        public Set<Class<? extends SpecificationValidator>> requires() {
            return Set.of(C.class);
        }

        @Override
        public boolean report(Builder builder) {
            if (reportsError) {
                builder.addError("A/path", "A/code", "A/error message");
            }
            return reportsError;
        }

        @Override
        public String toString() {
            return "A";
        }
    }

    static class B implements SpecificationValidator {

        private final boolean reportsError;

        public B() {
            this(false);
        }

        public B(boolean reportsError) {
            this.reportsError = reportsError;
        }

        @Override
        public Set<Class<? extends SpecificationValidator>> requires() {
            return Set.of(A.class);
        }

        @Override
        public boolean report(Builder builder) {
            if (reportsError) {
                builder.addError("B/path", "B/code", "B/error message");
            }
            return reportsError;
        }

        @Override
        public String toString() {
            return "B";
        }
    }

    static class C implements SpecificationValidator {

        private final boolean reportsError;

        public C() {
            this(false);
        }

        public C(boolean reportsError) {
            this.reportsError = reportsError;
        }

        @Override
        public boolean report(Builder builder) {
            if (reportsError) {
                builder.addError("C/path", "C/code", "C/error message");
            }
            return reportsError;
        }

        @Override
        public String toString() {
            return "C";
        }
    }

    static class D implements SpecificationValidator {

        @Override
        public Set<Class<? extends SpecificationValidator>> requires() {
            return Set.of(B.class);
        }

        @Override
        public boolean report(Builder builder) {
            return false;
        }

        @Override
        public String toString() {
            return "D";
        }
    }

    static class E implements SpecificationValidator {

        @Override
        public Set<Class<? extends SpecificationValidator>> requires() {
            return Set.of(F.class);
        }

        @Override
        public boolean report(Builder builder) {
            return false;
        }

        @Override
        public String toString() {
            return "E";
        }
    }

    static class F implements SpecificationValidator {

        @Override
        public Set<Class<? extends SpecificationValidator>> requires() {
            return Set.of(E.class);
        }

        @Override
        public boolean report(Builder builder) {
            return false;
        }

        @Override
        public String toString() {
            return "F";
        }
    }

    private static ImportSpecification aSpec() {
        var aSource = new BigQuerySource("a-source", "SELECT 42");
        var aTarget = new CustomQueryTarget(
                true, "a-target", "a-source", null, "UNWIND $rows AS row CREATE (n:Node) n = row");
        return new ImportSpecification("1.0", null, List.of(aSource), new Targets(null, null, List.of(aTarget)), null);
    }
}
