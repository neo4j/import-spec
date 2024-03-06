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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class CycleDetector {

    public static <T> List<List<T>> run(Map<T, T> graph) {
        List<List<T>> cycles = new ArrayList<>();
        Set<T> visitedNodes = new HashSet<>();

        for (T node : graph.keySet()) {
            if (visitedNodes.contains(node)) {
                continue;
            }
            List<T> path = new ArrayList<>();
            Stack<T> stack = new Stack<>();
            stack.push(node);

            while (!stack.isEmpty()) {
                T currentNode = stack.pop();
                path.add(currentNode);
                visitedNodes.add(currentNode);

                T dependency = graph.get(currentNode);
                if (dependency == null) {
                    continue;
                }
                int dependencyIndex = path.indexOf(dependency);
                if (dependencyIndex > -1) {
                    List<T> cycle = new ArrayList<>(path.subList(dependencyIndex, path.size()));
                    cycles.add(cycle);
                } else if (!visitedNodes.contains(dependency)) {
                    stack.push(dependency);
                }
            }
        }

        return cycles;
    }
}
