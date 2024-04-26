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
package org.neo4j.importer.v1.validation.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.graph.Graph;
import org.neo4j.importer.v1.graph.Pair;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDependencyCycleValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "CYCL-001";

    private final Map<Element, List<String>> dependencyMatrix;
    private final Map<String, String> namedPaths;

    public NoDependencyCycleValidator() {
        dependencyMatrix = new LinkedHashMap<>();
        namedPaths = new HashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDuplicatedTargetActionNameValidator.class,
                NoDanglingDependsOnValidator.class,
                NoDanglingNodeReferenceValidator.class,
                NoDuplicatedDependencyValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        trackDependency(target, String.format("$.targets.nodes[%d]", index));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        trackDependency(target, String.format("$.targets.relationships[%d]", index));
    }

    @Override
    public void visitCustomQueryTarget(int index, CustomQueryTarget target) {
        trackDependency(target, String.format("$.targets.queries[%d]", index));
    }

    @Override
    public boolean report(Builder builder) {
        AtomicBoolean result = new AtomicBoolean(false);
        Graph.detectCycles(dependencyGraph()).stream()
                .map(cycle -> {
                    Element cycleStart = cycle.get(0);
                    String cycleDescription = cycle.stream()
                            .map(Element::getName)
                            .reduce((element1, element2) -> String.format("%s->%s", element1, element2))
                            .get();
                    String closedCycleDescription = String.format("%s->%s", cycleDescription, cycleStart.getName());
                    return Pair.of(cycleStart.getPath(), closedCycleDescription);
                })
                .forEach((cycle) -> {
                    result.set(true);
                    builder.addError(
                            cycle.getFirst(),
                            ERROR_CODE,
                            String.format("A dependency cycle has been detected: %s", cycle.getSecond()));
                });
        return result.get();
    }

    private void trackDependency(RelationshipTarget target, String path) {
        trackDependency((Target) target, path);
        String startNodeRef = target.getStartNodeReference();
        if (startNodeRef != null) {
            addDependencies(new Element(target.getName(), path), List.of(startNodeRef));
        }
        String endNodeRef = target.getEndNodeReference();
        if (endNodeRef != null) {
            addDependencies(new Element(target.getName(), path), List.of(endNodeRef));
        }
    }

    private void trackDependency(Target target, String path) {
        String targetName = target.getName();
        namedPaths.put(targetName, path);
        addDependencies(new Element(targetName, path), target.getDependencies());
    }

    private Map<Element, Set<Element>> dependencyGraph() {
        var dependencyGraph = new LinkedHashMap<Element, Set<Element>>(dependencyMatrix.size());
        for (Element key : dependencyMatrix.keySet()) {
            Set<Element> dependencies = dependencyMatrix.get(key).stream()
                    .map(name -> new Element(name, namedPaths.get(name)))
                    .collect(Collectors.toCollection(() -> new LinkedHashSet<>(3)));
            dependencyGraph.put(key, dependencies);
        }
        return dependencyGraph;
    }

    private void addDependencies(Element key, List<String> dependencyNames) {
        dependencyMatrix.computeIfAbsent(key, (ignored) -> new ArrayList<>()).addAll(dependencyNames);
    }
}

class Element {
    private final String name;
    private final String path;

    public Element(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Element element = (Element) object;
        return Objects.equals(name, element.name) && Objects.equals(path, element.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }

    @Override
    public String toString() {
        return "Element{" + "name='" + name + '\'' + ", path='" + path + '\'' + '}';
    }
}
