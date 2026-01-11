package com.example.productAnalytics.writeStrategy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IWriteStrategy {
    CompletableFuture<Void> write(Supplier<CompletableFuture<Void>> cacheWrite, Supplier<CompletableFuture<Void>> dbWrite);
}
