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
package org.neo4j.importer;

import java.util.concurrent.CompletableFuture;

class CompletableFutures {

    public static <T> CompletableFuture<T> initial() {
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> mergeSequential(
            CompletableFuture<Void> chain1, CompletableFuture<Void> chain2) {
        return chain1.thenCompose(v -> chain2);
    }

    public static CompletableFuture<Void> mergeParallel(
            CompletableFuture<Void> chain1, CompletableFuture<Void> chain2) {
        return CompletableFuture.allOf(chain1, chain2);
    }
}
