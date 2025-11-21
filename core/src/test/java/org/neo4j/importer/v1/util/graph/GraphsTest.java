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
package org.neo4j.importer.v1.util.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.util.graph.TopologicalSortAsserter.topologicalSortOf;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class GraphsTest {

    @Test
    void topologically_sorts_dependencyless_dependency_graph() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", Set.of());
        graph.put("b", Set.of());
        graph.put("c", Set.of());

        List<String> result = Graphs.runTopologicalSort(graph);

        assertThat(result).satisfies(topologicalSortOf(graph));
    }

    @Test
    void topologically_sorts_dependency_graph() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("b", "c"));
        graph.put("c", linkedHashSet("d", "e", "f"));
        graph.put("e", linkedHashSet("g"));

        List<String> result = Graphs.runTopologicalSort(graph);

        assertThat(result).satisfies(topologicalSortOf(graph));
    }

    @Test
    void fails_to_topologically_sort_cyclic_dependency_graph() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("b"));
        graph.put("b", linkedHashSet("a"));

        assertThatThrownBy(() -> Graphs.runTopologicalSort(graph))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The provided graph {a=[b], b=[a]} defines cycles");
    }

    @Test
    void fails_to_topologically_sort_dependency_graph_with_long_cycles() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("b"));
        graph.put("b", linkedHashSet("c"));
        graph.put("c", linkedHashSet("d"));
        graph.put("d", linkedHashSet("e"));
        graph.put("e", linkedHashSet("f"));
        graph.put("f", linkedHashSet("g"));
        graph.put("g", linkedHashSet("a"));

        assertThatThrownBy(() -> Graphs.runTopologicalSort(graph))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "The provided graph {a=[b], b=[c], c=[d], d=[e], e=[f], f=[g], g=[a]} defines cycles");
    }

    @Test
    void detects_no_cycles_for_empty_graph_with_complex_graph() {
        var cycles = Graphs.detectCycles(new LinkedHashMap<>());

        assertThat(cycles).isEmpty();
    }

    @Test
    void detects_no_cycles_for_dag_with_complex_graph() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("b", "c"));
        graph.put("c", linkedHashSet("d", "e", "f"));
        graph.put("e", linkedHashSet("g"));

        var cycles = Graphs.detectCycles(graph);

        assertThat(cycles).isEmpty();
    }

    @Test
    void detects_direct_cycles_with_complex_graph() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("a", "b"));
        graph.put("b", linkedHashSet("b", "c"));

        var cycles = Graphs.detectCycles(graph);

        assertThat(cycles).isEqualTo(List.of(List.of("a"), List.of("b")));
    }

    @Test
    void detects_cycles_with_complex_graph() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("b", "c"));
        graph.put("c", linkedHashSet("a", "d"));

        var cycles = Graphs.detectCycles(graph);

        assertThat(cycles).isEqualTo(List.of(List.of("a", "c")));
    }

    @Test
    void finds_weakly_connected_components() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("a", linkedHashSet("b", "c"));
        graph.put("d", linkedHashSet("e", "f"));
        graph.put("g", linkedHashSet("f"));
        graph.put("h", linkedHashSet("i"));

        var components = Graphs.findWeaklyConnectedComponents(graph);

        assertThat(components).isEqualTo(List.of(Set.of("a", "b", "c"), Set.of("d", "e", "f", "g"), Set.of("h", "i")));
    }

    @Test
    void finds_no_weakly_connected_components_for_empty_graph() {
        assertThat(Graphs.findWeaklyConnectedComponents(new LinkedHashMap<String, Set<String>>()))
                .isEmpty();
    }

    @SafeVarargs
    private static <T> Set<T> linkedHashSet(T... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    }
}

class TopologicalSortAsserter<T> implements ThrowingConsumer<List<? extends T>> {

    private final Map<T, Set<T>> graph;

    private TopologicalSortAsserter(Map<T, Set<T>> graph) {
        this.graph = graph;
    }

    public static <T> TopologicalSortAsserter<T> topologicalSortOf(Map<T, Set<T>> graph) {
        return new TopologicalSortAsserter<>(graph);
    }

    @Override
    public void acceptThrows(List<? extends T> result) {
        graph.forEach((dependent, dependencies) -> dependencies.forEach((dependency) -> {
            int dependentIndex = result.indexOf(dependent);
            int dependencyIndex = result.indexOf(dependency);
            assertThat(dependentIndex)
                    .overridingErrorMessage(
                            "%s depends on %s, its index (%d) should be larger than its dependency's (%d)",
                            dependent, dependency, dependentIndex, dependencyIndex)
                    .isGreaterThan(dependencyIndex);
        }));
    }
}
