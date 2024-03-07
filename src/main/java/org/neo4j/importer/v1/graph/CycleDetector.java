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
package org.neo4j.importer.v1.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CycleDetector {

    public static <T> List<List<T>> detectSimple(Map<T, T> matrix) {
        return detect(mapValues(matrix, CycleDetector::valueAsSet));
    }

    public static <T> List<List<T>> detect(Map<T, Set<T>> matrix) {
        List<List<T>> cycles = new ArrayList<>();
        Set<T> visitedNodes = new HashSet<>();

        for (T node : matrix.keySet()) {
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

                Set<T> dependencies = matrix.get(currentNode);
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

    private static <T> Set<T> valueAsSet(Entry<T, T> entry) {
        T value = entry.getValue();
        if (value == null) {
            return Set.of();
        }
        return Set.of(value);
    }

    private static <T, V> Map<T, V> mapValues(Map<T, T> graph, Function<Entry<T, T>, V> valueMapper) {
        return graph.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        valueMapper,
                        (x, y) -> {
                            throw new RuntimeException("did not expect duplicated keys");
                        },
                        () -> new LinkedHashMap<>(graph.size())));
    }
}
