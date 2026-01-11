package com.example.productAnalytics.writeStrategy;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class WriteBackStrategy implements IWriteStrategy {
    @Override
    public CompletableFuture<Void> write(Supplier<CompletableFuture<Void>> cacheWrite, Supplier<CompletableFuture<Void>> dbWrite) {
        return cacheWrite.get()
                .whenComplete((v, e) -> {
                    if (e == null) {
                        dbWrite.get(); // fire and forget
                    }
                });
    }
}
