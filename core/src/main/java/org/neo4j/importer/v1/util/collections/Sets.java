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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Sets {

    /**
     * Returns all elements of the first set not contained in set2
     * @param set1 first set
     * @param set2 second set
     * @return elements of set1 not in set2
     * @param <T> type of elements
     */
    public static <T> Set<T> difference(Set<T> set1, Set<T> set2) {
        return set1.stream().filter(element -> !set2.contains(element)).collect(Collectors.toSet());
    }

    /**
     * Returns all elements of the first set contained in set2
     * @param set1 first set
     * @param set2 second set
     * @return elements of set1 not in set2
     * @param <T> type of elements
     */
    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        return set1.stream().filter(set2::contains).collect(Collectors.toSet());
    }

    /**
     * @see Sets#generateNonEmptySubsets(Set, Comparator)
     */
    public static <T> Stream<Set<T>> generateNonEmptySubsets(Set<T> inputSet) {
        return generateNonEmptySubsets(inputSet, null);
    }

    /**
     * This generates a {@code Stream} of all non-empty subsets of the provided input set.
     * <p>
     * The method optionally accepts a {@code Comparator<Set<T>>} to sort the resulting
     * stream of subsets. If the comparator is {@code null}, the subsets are returned
     * in the order they are generated.
     * <p>
     * This method has an exponential time complexity of O(n * 2^n), where n is the size
     * of the input set. Please call this sparingly.
     *
     * @param <T> type of inputs.
     * @param inputSet inputs
     * @param order optional {@code Comparator<Set<T>>} order to sort subsets with. If {@code null}, no ordering is applied.
     * @return {@code Stream<Set<T>>} containing all 2^n - 1 non-empty subsets, optionally ordered.
     */
    public static <T> Stream<Set<T>> generateNonEmptySubsets(Set<T> inputSet, Comparator<Set<T>> order) {
        var elements = new ArrayList<>(inputSet);
        var n = elements.size();
        if (n == 0) {
            return Stream.empty();
        }

        Stream<Set<T>> result = IntStream.range(1, 1 << n /* 2^n unordered subsets */)
                .mapToObj(subsetIndex -> IntStream.range(0, n)
                        /*
                         * The bitwise subset generation method maps each element in the input list
                         * to a specific bit position (inputIndex) and uses an integer 'subsetIndex' to
                         * determine which elements to include.
                         *
                         * Given inputSet = ["Apple" (inputIndex=0),
                         *                   "Banana" (inputIndex=1),
                         *                   "Cherry" (inputIndex=2)],
                         * the generation loop iterates through 'subsetIndex' from 1 to 7 (2^3 - 1).
                         *
                         * The filter condition is: {@code (subsetIndex & (1 << inputIndex)) != 0}
                         *
                         * ----------------------------------------------------------------------------------------------------------------------------
                         * | subsetIndex | Binary | Bits Set            | inputIndex=0   | inputIndex=1 | inputIndex=2 | Subset
                         * | --------------------------------------------------------------------------------------------------------------------------
                         * | 1          | 001    | inputIndex=0         | YES            | NO           | NO           | {"Apple"}
                         * | 2          | 010    | inputIndex=1         | NO             | YES          | NO           | {"Banana"}
                         * | 3          | 011    | inputIndex=0, 1      | YES            | YES          | NO           | {"Apple", "Banana"}
                         * | 4          | 100    | inputIndex=2         | NO             | NO           | YES          | {"Cherry"}
                         * | 5          | 101    | inputIndex=0, 2      | YES            | NO           | YES          | {"Apple", "Cherry"}
                         * | 6          | 110    | inputIndex=1, 2      | NO             | YES          | YES          | {"Banana", "Cherry"}
                         * | 7          | 111    | inputIndex=0, 1, 2   | YES            | YES          | YES          | {"Apple", "Banana", "Cherry"}
                         * ----------------------------------------------------------------------------------------------------------------------------
                         */
                        .filter(inputIndex -> (subsetIndex & (1 << inputIndex)) != 0)
                        .mapToObj(elements::get)
                        .collect(Collectors.toSet()));

        if (order != null) {
            return result.sorted(order);
        }
        return result;
    }
}
