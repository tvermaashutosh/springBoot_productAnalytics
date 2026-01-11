package com.example.productAnalytics.writeStrategy;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class WriteThroughStrategy implements IWriteStrategy {
    private final Map<String, Object> globalConfig;

    @Override
    public CompletableFuture<Void> write(Supplier<CompletableFuture<Void>> cacheWrite, Supplier<CompletableFuture<Void>> dbWrite) {
        if (globalConfig.get("parallelWriteThrough").equals(true)) {
            return CompletableFuture.allOf(cacheWrite.get(), dbWrite.get());
        }

        return cacheWrite.get()
                .thenCompose(v -> dbWrite.get());
    }
}
