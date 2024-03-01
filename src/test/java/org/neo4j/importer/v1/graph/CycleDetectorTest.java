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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CycleDetectorTest {

    @Test
    void detects_no_cycles_for_empty_graph() {
        var cycles = CycleDetector.run(linkedHashMap());

        assertThat(cycles).isEmpty();
    }

    @Test
    void detects_direct_cycles() {
        var cycles = CycleDetector.run(linkedHashMap("task1", "task1", "task2", "task2"));

        assertThat(cycles).isEqualTo(List.of(List.of(Pair.of("task1", "task1")), List.of(Pair.of("task2", "task2"))));
    }

    @Test
    void detects_no_cycles_for_dag() {
        var cycles = CycleDetector.run(linkedHashMap("task1", "task2", "task2", "task3"));

        assertThat(cycles).isEmpty();
    }

    @Test
    void detects_long_cycles() {
        var cycles = CycleDetector.run(linkedHashMap(
                "task1", "task2", "task2", "task3", "task3", "task1", "task5", "task6", "task7", "task8", "task8",
                "task9", "task9", "task10", "task10", "task11", "task11", "task7", "task12", "task13", "task13",
                "task1"));

        assertThat(cycles)
                .isEqualTo(List.of(
                        List.of(Pair.of("task1", "task2"), Pair.of("task2", "task3"), Pair.of("task3", "task1")),
                        List.of(
                                Pair.of("task7", "task8"),
                                Pair.of("task8", "task9"),
                                Pair.of("task9", "task10"),
                                Pair.of("task10", "task11"),
                                Pair.of("task11", "task7"))));
    }

    @SafeVarargs
    private static <T> Map<T, T> linkedHashMap(T... elements) {
        if (elements.length % 2 != 0) {
            Assertions.fail("expected even number of key-value elements, got: %d", elements.length);
        }
        var result = new LinkedHashMap<T, T>(elements.length / 2);
        for (int i = 0; i < elements.length; i += 2) {
            result.put(elements[i], elements[i + 1]);
        }
        return result;
    }
}
