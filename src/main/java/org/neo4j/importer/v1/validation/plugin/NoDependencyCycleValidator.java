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
package org.neo4j.importer.v1.validation.plugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.graph.CycleDetector;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoDependencyCycleValidator implements SpecificationValidator {

    private final Map<Element, String> dependencies;
    private final Map<String, String> namedPaths;

    public NoDependencyCycleValidator() {
        dependencies = new LinkedHashMap<>();
        namedPaths = new HashMap<>();
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
    public void visitAction(int index, Action action) {
        trackDependency(action, String.format("$.actions[%d]", index));
    }

    @Override
    public void accept(Builder builder) {
        Map<Element, Element> graph = dependencyGraph();
        CycleDetector.run(graph).forEach((cycle) -> {
            String cycleDescription = cycle.stream()
                    .map(pair -> {
                        Element dependent = pair.getFirst();
                        Element dependency = pair.getSecond();
                        return String.format(
                                "\"%s\" (%s) depends on \"%s\" (%s)",
                                dependent.getName(), dependent.getPath(), dependency.getName(), dependency.getPath());
                    })
                    .collect(Collectors.joining("\n\t\t- ", "\t\t- ", ""));
            Element cycleStart = cycle.get(0).getFirst();
            builder.addError(
                    cycleStart.getPath(),
                    "CYCL-001",
                    String.format("A dependency cycle has been detected:%n %s", cycleDescription));
        });
    }

    private void trackDependency(Target target, String path) {
        String targetName = target.getName();
        namedPaths.put(targetName, path);
        String dependencyName = target.getDependsOn();
        if (dependencyName != null) {
            dependencies.put(new Element(targetName, path), dependencyName);
        }
    }

    private void trackDependency(Action action, String path) {
        String targetName = action.getName();
        namedPaths.put(targetName, path);
        String dependencyName = action.getDependsOn();
        if (dependencyName != null) {
            dependencies.put(new Element(targetName, path), dependencyName);
        }
    }

    private Map<Element, Element> dependencyGraph() {
        var dependencyGraph = new LinkedHashMap<Element, Element>();
        for (Element key : dependencies.keySet()) {
            String dependencyName = dependencies.get(key);
            dependencyGraph.put(key, new Element(dependencyName, namedPaths.get(dependencyName)));
        }
        return dependencyGraph;
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
