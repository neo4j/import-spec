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

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.graph.Graphs;

public class SpecificationValidators {

    private final List<SpecificationValidator> validators;

    private SpecificationValidators(List<SpecificationValidator> validators) {
        this.validators = validators;
    }

    public static SpecificationValidators of(List<SpecificationValidator> validators) {
        return new SpecificationValidators(runTopogicalSort(validators));
    }

    public static SpecificationValidators of(SpecificationValidator validator) {
        return new SpecificationValidators(List.of(validator));
    }

    public void validate(ImportSpecification spec) throws SpecificationException {
        validators.forEach(validator -> validator.visitConfiguration(spec.getConfiguration()));
        var sources = spec.getSources();
        for (int i = 0; i < sources.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitSource(index, sources.get(index)));
        }
        var targets = spec.getTargets();
        var nodeTargets = targets.getNodes();
        for (int i = 0; i < nodeTargets.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitNodeTarget(index, nodeTargets.get(index)));
        }
        var relationshipTargets = targets.getRelationships();
        for (int i = 0; i < relationshipTargets.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitRelationshipTarget(index, relationshipTargets.get(index)));
        }
        var queryTargets = targets.getCustomQueries();
        for (int i = 0; i < queryTargets.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitCustomQueryTarget(index, queryTargets.get(index)));
        }
        var actions = spec.getActions();
        for (int i = 0; i < actions.size(); i++) {
            final int index = i;
            validators.forEach(validator -> validator.visitAction(index, actions.get(index)));
        }

        Set<Class<? extends SpecificationValidator>> failedValidations = new HashSet<>(validators.size());
        Map<Class<? extends SpecificationValidator>, SpecificationValidator> validatorsPerClass =
                validators.stream().collect(toMap(SpecificationValidator::getClass, Function.identity()));
        var builder = SpecificationValidationResult.builder();
        validators.forEach(validator -> {
            for (Class<? extends SpecificationValidator> dependent :
                    resolveTransitiveRequires(validator, validatorsPerClass)) {
                if (failedValidations.contains(dependent)) {
                    return;
                }
            }
            if (validator.report(builder)) {
                failedValidations.add(validator.getClass());
            }
        });
        SpecificationValidationResult result = builder.build();
        if (!result.passes()) {
            throw new InvalidSpecificationException(result);
        }
    }

    // visible for testing
    List<SpecificationValidator> getValidators() {
        return validators;
    }

    private static List<SpecificationValidator> runTopogicalSort(List<SpecificationValidator> validators) {
        var validatorCatalog = new HashMap<Class<? extends SpecificationValidator>, SpecificationValidator>();
        var validatorGraph =
                new HashMap<Class<? extends SpecificationValidator>, Set<Class<? extends SpecificationValidator>>>();
        validators.forEach(validator -> {
            validatorCatalog.put(validator.getClass(), validator);
            validatorGraph.put(validator.getClass(), validator.requires());
        });
        return Graphs.runTopologicalSort(validatorGraph).stream()
                .map(validatorCatalog::get)
                .collect(Collectors.toList());
    }

    private static Set<Class<? extends SpecificationValidator>> resolveTransitiveRequires(
            SpecificationValidator validator,
            Map<Class<? extends SpecificationValidator>, SpecificationValidator> dependenciesPerClass) {
        Set<Class<? extends SpecificationValidator>> dependencyClasses = validator.requires();
        var result = new HashSet<Class<? extends SpecificationValidator>>();
        Stack<Class<? extends SpecificationValidator>> stack = new Stack<>();
        stack.addAll(dependencyClasses);
        while (!stack.isEmpty()) {
            var dependencyClass = stack.pop();
            result.add(dependencyClass);
            var dependency = dependenciesPerClass.get(dependencyClass);
            stack.addAll(dependency.requires());
        }
        return result;
    }
}
