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
package org.neo4j.importer.v1.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Graphs {

    /**
     * Returns a topologically-sorted list from a given dependency graph
     * The Map encodes dependent-to-dependencies mappings.
     * - Map.of("a", Set.of("b", "c")) means item a depends on both item b and c
     * - If "b" is listed as a dependency, the absence of "b" from the key set or a mapping to an empty set of dependencies is equivalent
     *
     * @param graph the dependency graph
     * @param <T>   the element type
     * @return a topologically-sorted list
     */
    public static <T> List<T> runTopologicalSort(Map<T, Set<T>> graph) { // K: dependent, V: dependencies
        Map<T, Integer> outDegrees = getAllValues(graph)
                .collect(Collectors.toMap(Function.identity(), value -> graph.getOrDefault(value, Set.of())
                        .size()));
        Map<T, Set<T>> dependencies = reverseGraph(graph); // K: dependency, V: dependents
        Queue<T> queue = new ArrayDeque<>(graph.size());
        for (T node : outDegrees.keySet()) {
            if (outDegrees.getOrDefault(node, 0) == 0) {
                queue.add(node);
            }
        }

        List<T> result = new ArrayList<>(graph.size());
        while (!queue.isEmpty()) {
            T node = queue.poll();
            result.add(node);
            for (T dependent : dependencies.getOrDefault(node, Set.of())) {
                outDegrees.put(dependent, outDegrees.get(dependent) - 1);
                if (outDegrees.get(dependent) == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (result.size() < outDegrees.size()) {
            throw new IllegalArgumentException(String.format("The provided graph %s defines cycles", graph));
        }
        return result;
    }

    /**
     * Detects whether the given dependency graph encoded as a map contains any cycles.
     * The Map encodes dependent-to-dependencies mappings.<br>
     * - Map.of("a", Set.of("b", "c")) means item a depends on both item b and c<br>
     * - Map.of("b", Set.of()) means item b does not have any dependencies<br>
     * @param graph the dependency graph
     * @param <T>   the element type
     * @return the list of paths with cycles, an empty list means there is no cycle
     */
    public static <T> List<List<T>> detectCycles(Map<T, Set<T>> graph) {
        List<List<T>> cycles = new ArrayList<>();
        Set<T> visitedNodes = new HashSet<>();

        for (T node : graph.keySet()) {
            if (visitedNodes.contains(node)) {
                continue;
            }
            Map<T, List<T>> paths = new HashMap<>();
            Queue<T> queue = new ArrayDeque<>();
            queue.add(node);

            while (!queue.isEmpty()) {
                T currentNode = queue.poll();
                List<T> path = paths.computeIfAbsent(currentNode, (key) -> new ArrayList<>());
                path.add(currentNode);
                visitedNodes.add(currentNode);

                Set<T> dependencies = graph.get(currentNode);
                if (dependencies == null) {
                    continue;
                }
                for (T dependency : dependencies) {
                    int dependencyIndex = path.indexOf(dependency);
                    if (dependencyIndex > -1) {
                        List<T> cycle = new ArrayList<>(path.subList(dependencyIndex, path.size()));
                        cycles.add(cycle);
                    } else if (!visitedNodes.contains(dependency)) {
                        queue.add(dependency);
                        paths.remove(currentNode);
                        paths.put(dependency, new ArrayList<>(path));
                    }
                }
            }
        }
        return cycles;
    }

    private static <T> Stream<T> getAllValues(Map<T, Set<T>> graph) {
        return graph.entrySet().stream()
                .flatMap(entry -> mergeKeyValues(entry).stream())
                .distinct();
    }

    private static <T> Set<T> mergeKeyValues(Entry<T, Set<T>> entry) {
        Set<T> result = new HashSet<>();
        result.add(entry.getKey());
        result.addAll(entry.getValue());
        return result;
    }

    private static <T> Map<T, Set<T>> reverseGraph(Map<T, Set<T>> graph) {
        Map<T, Set<T>> dependents = new HashMap<>(graph.size());
        graph.forEach((dependent, dependencies) -> dependencies.forEach(dependency ->
                dependents.computeIfAbsent(dependency, (key) -> new HashSet<>()).add(dependent)));
        return dependents;
    }
}
