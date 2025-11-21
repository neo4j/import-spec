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
package org.neo4j.importer.v1.util.collections;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SetsTest {

    @Test
    void provides_no_subsets_for_empty_set() {
        var result = Sets.<String>generateNonEmptySubsets(Set.of());

        assertThat(result.collect(toSet())).isEmpty();
    }

    @Test
    void provides_all_subsets() {
        var result = Sets.generateNonEmptySubsets(Set.of("a", "b", "c"));

        assertThat(result.collect(toSet()))
                .containsExactlyInAnyOrder(
                        Set.of("a"),
                        Set.of("b"),
                        Set.of("c"),
                        Set.of("a", "b"),
                        Set.of("a", "c"),
                        Set.of("b", "c"),
                        Set.of("a", "b", "c"));
    }

    @Test
    void provides_all_subsets_in_specified_order() {
        var bySizeDesc = Comparator.<Set<String>>comparingInt(Set::size).reversed();

        var result = Sets.generateNonEmptySubsets(Set.of("a", "b", "c"), bySizeDesc);

        var elements = result.collect(toList());
        assertThat(elements)
                .containsExactlyInAnyOrder(
                        Set.of("a", "b", "c"),
                        Set.of("a", "b"),
                        Set.of("a", "c"),
                        Set.of("b", "c"),
                        Set.of("a"),
                        Set.of("b"),
                        Set.of("c"));
        assertThat(elements.getFirst()).hasSize(3);
        assertThat(elements.subList(1, 4)).allSatisfy(set -> assertThat(set).hasSize(2));
        assertThat(elements.subList(4, 7)).allSatisfy(set -> assertThat(set).hasSize(1));
    }
}
