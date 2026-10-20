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

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.graph.Graphs;
import org.neo4j.importer.v1.validation.InvalidDeserializerOptionsException;

public class InterpolationVariableValidators {

    private final List<InterpolationVariableValidator> validators;

    private InterpolationVariableValidators(List<InterpolationVariableValidator> validators) {
        this.validators = validators;
    }

    public static InterpolationVariableValidators of(List<InterpolationVariableValidator> validators) {
        return new InterpolationVariableValidators(runTopologicalSort(validators));
    }

    public static InterpolationVariableValidators of(InterpolationVariableValidator validator) {
        return new InterpolationVariableValidators(List.of(validator));
    }

    public void validate(Map<String, Object> variables) throws InvalidDeserializerOptionsException {
        Map<Class<? extends InterpolationVariableValidator>, InterpolationVariableValidator> validatorsPerClass =
                validators.stream().collect(toMap(InterpolationVariableValidator::getClass, Function.identity()));
        Set<Class<? extends InterpolationVariableValidator>> failedValidations = new HashSet<>(validators.size());

        var errorMessages = new ArrayList<String>();
        for (InterpolationVariableValidator validator : validators) {
            if (hasFailedRequirement(validator, validatorsPerClass, failedValidations)) {
                continue;
            }
            variables.forEach((name, value) -> {
                var result = validator.validate(name, value);
                if (!result.passes()) {
                    failedValidations.add(validator.getClass());
                    errorMessages.addAll(result.getErrors());
                }
            });
        }

        if (!errorMessages.isEmpty()) {
            throw new InvalidDeserializerOptionsException(
                    "Invalid interpolation variable definitions were found. See details below: %n%s",
                    String.join("\n", errorMessages));
        }
    }

    private static List<InterpolationVariableValidator> runTopologicalSort(
            List<InterpolationVariableValidator> validators) {
        var validatorCatalog =
                new HashMap<Class<? extends InterpolationVariableValidator>, InterpolationVariableValidator>();
        var validatorGraph = new HashMap<
                Class<? extends InterpolationVariableValidator>,
                Set<Class<? extends InterpolationVariableValidator>>>();
        validators.forEach(validator -> {
            validatorCatalog.put(validator.getClass(), validator);
            validatorGraph.put(validator.getClass(), validator.requires());
        });
        return Graphs.runTopologicalSort(validatorGraph).stream()
                .map(validatorCatalog::get)
                .collect(Collectors.toList());
    }

    private static Set<Class<? extends InterpolationVariableValidator>> resolveTransitiveRequires(
            InterpolationVariableValidator validator,
            Map<Class<? extends InterpolationVariableValidator>, InterpolationVariableValidator> dependenciesPerClass) {
        Set<Class<? extends InterpolationVariableValidator>> dependencyClasses = validator.requires();
        var result = new HashSet<Class<? extends InterpolationVariableValidator>>();
        Stack<Class<? extends InterpolationVariableValidator>> stack = new Stack<>();
        stack.addAll(dependencyClasses);
        while (!stack.isEmpty()) {
            var dependencyClass = stack.pop();
            if (!result.add(dependencyClass)) {
                // already processed this requirement, do not expand it again
                continue;
            }
            var dependency = dependenciesPerClass.get(dependencyClass);
            if (dependency != null) {
                // the dependency may be absent if a validator requires another one that was not provided
                stack.addAll(dependency.requires());
            }
        }
        return result;
    }

    private static boolean hasFailedRequirement(
            InterpolationVariableValidator validator,
            Map<Class<? extends InterpolationVariableValidator>, InterpolationVariableValidator> dependenciesPerClass,
            Set<Class<? extends InterpolationVariableValidator>> failedValidations) {
        for (Class<? extends InterpolationVariableValidator> requirement :
                resolveTransitiveRequires(validator, dependenciesPerClass)) {
            if (failedValidations.contains(requirement)) {
                return true;
            }
        }
        return false;
    }
}
