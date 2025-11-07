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
package org.neo4j.importer.v1.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.graph.Graphs;

/**
 * {@link ImportExecutionPlan} exposes the graph of {@link ImportStep} to execute in a way that eases import
 * parallelization.<br><br>
 * The first level of parallelization is {@link org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepGroup},
 * retrieved with {@link ImportExecutionPlan#getGroups()}.
 * Each group corresponds to a weakly connected component of the import step graph.<br>
 * For instance, the following YAML serialization of {@link org.neo4j.importer.v1.ImportSpecification} (other attributes
 * are omitted for brevity):
 * <pre><code>
 * version: "1"
 * sources:
 *   - name: actors
 *   - name: films
 * targets:
 *   nodes:
 *     - source: actors
 *       name: actor_nodes
 *     - source: films
 *       name: film_nodes
 * </code></pre>
 * <br>
 * ... results into 2 groups:<br><br>
 *  - 1 with the "actors" source and "actor_nodes" node target (converted respectively to {@link SourceStep} and
 *  {@link NodeTargetStep})<br>
 *  - 1 with the "films" source and "film_nodes" node target (converted respectively to {@link SourceStep} and
 *  {@link NodeTargetStep})<br>
 *  <br>
 *  These groups can be processed in parallel.
 *  The import is considered completed when every group's import has completed.<br><br>
 *  Each {@link org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepGroup} is made of several
 *  {@link org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepStage}, retrieved with
 *  {@link ImportStepGroup#getStages()}.<br>
 *  Stages <strong>must</strong> be processed sequentially. In other words, the second stage can not run until the first
 *  stage has completed, and so on.<br>
 *  <br>
 *  Assuming the following YAML serialization of {@link org.neo4j.importer.v1.ImportSpecification} (other attributes are
 *  omitted for brevity):
 * <pre><code>
 * version: "1"
 * sources:
 *   - name: actors
 *   - name: films
 *   - name: actors_in_films
 * targets:
 *   nodes:
 *     - source: actors
 *       name: actor_nodes
 *     - source: films
 *       name: film_nodes
 *   relationships:
 *     - source: actors_in_films
 *       name: actor_film_relationships
 *       start_node_reference: actor_nodes
 *       end_node_reference: film_nodes
 * </code></pre>
 * This would result in a single {@link org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepGroup}
 * (every step is linked, directly or indirectly).
 * The group is made of at least 3 stages:<br>
 *  - the first stage includes all the sources<br>
 *  - the second stage includes all the nodes<br>
 *  - the last stage includes the relationship<br>
 *  <br>
 *  Finally, each stage is made of several steps.
 *  These steps (either {@link SourceStep}, {@link NodeTargetStep}, {@link RelationshipTargetStep},
 *  {@link CustomQueryTargetStep} or {@link ActionStep}) can be processed in parallel.<br>
 *  The enclosing stage execution is considered complete when all its steps have completed.
 */
public class ImportExecutionPlan {

    private final List<ImportStepGroup> groups;

    ImportExecutionPlan(List<ImportStepGroup> groups) {
        this.groups = groups;
    }

    static ImportExecutionPlan fromGraph(Map<ImportStep, Set<ImportStep>> dependencyGraph) {
        var components = Graphs.findWeaklyConnectedComponents(dependencyGraph);
        var groups = new ArrayList<ImportStepGroup>();
        components.forEach((component) -> {
            var stages = new ArrayList<ImportStepStage>();
            // "out" as in (a:Step)-[:DEPENDS_ON]->(b:Step) (i.e. "out" from a's perspective)
            // a is a "dependent", b is a dependency, b needs to run first
            var outDegrees = new HashMap<ImportStep, Long>();
            component.forEach(dependent -> {
                var dependencyCount = dependencyGraph
                        .getOrDefault(dependent, Collections.emptySet())
                        .size();
                outDegrees.merge(dependent, (long) dependencyCount, Long::sum);
            });
            var reverseDependencyGraph = reverseDependencyGraph(dependencyGraph);
            while (true) {
                var currentStageSteps = outDegrees.entrySet().stream()
                        // tasks without dependencies are our current stage's starting points
                        .filter(entry -> entry.getValue() == 0)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (currentStageSteps.isEmpty()) {
                    break;
                }
                stages.add(new ImportStepStage(currentStageSteps));
                currentStageSteps.forEach(step -> {
                    outDegrees.remove(step);
                    var dependents = reverseDependencyGraph.getOrDefault(step, Collections.emptySet());
                    dependents.forEach(dependency -> {
                        outDegrees.compute(dependency, (key, value) -> value - 1);
                    });
                });
            }
            groups.add(new ImportStepGroup(stages));
        });
        return new ImportExecutionPlan(groups);
    }

    public List<ImportStepGroup> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "ImportExecutionPlan{" + "groups=" + groups + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImportExecutionPlan)) return false;
        ImportExecutionPlan that = (ImportExecutionPlan) o;
        return Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groups);
    }

    private static Map<ImportStep, Set<ImportStep>> reverseDependencyGraph(
            Map<ImportStep, Set<ImportStep>> dependencyGraph) {
        var reverseDependencyGraph = new HashMap<ImportStep, Set<ImportStep>>(dependencyGraph.size());
        dependencyGraph.forEach((dependent, dependencies) -> dependencies.forEach(dependency -> reverseDependencyGraph
                .computeIfAbsent(dependency, k -> new LinkedHashSet<>())
                .add(dependent)));
        return reverseDependencyGraph;
    }

    /**
     * Represents an independent group of related tasks (a connected component).
     * The tasks are organized into sequential stages, ImportStepStage, where all tasks in a
     * single stage can be executed in parallel.
     */
    public static class ImportStepGroup {

        private final List<ImportStepStage> stages;

        public ImportStepGroup(List<ImportStepStage> stages) {
            this.stages = stages;
        }

        public List<ImportStepStage> getStages() {
            return stages;
        }

        @Override
        public String toString() {
            return "ImportStepGroup{" + "steps=" + stages + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ImportStepGroup)) return false;
            ImportStepGroup that = (ImportStepGroup) o;
            return Objects.equals(stages, that.stages);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(stages);
        }
    }

    /**
     * Represents a single stage of execution containing a set of tasks
     * that can all run in parallel.
     */
    public static class ImportStepStage {

        private final Set<ImportStep> steps;

        public ImportStepStage(Set<ImportStep> steps) {
            this.steps = steps;
        }

        public Set<ImportStep> getSteps() {
            return steps;
        }

        @Override
        public String toString() {
            return "ImportStepStage{" + "steps=" + steps + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ImportStepStage)) return false;
            ImportStepStage that = (ImportStepStage) o;
            return Objects.equals(steps, that.steps);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(steps);
        }
    }
}
