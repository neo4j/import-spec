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
