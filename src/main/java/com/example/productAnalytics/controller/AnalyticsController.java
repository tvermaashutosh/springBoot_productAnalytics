package com.example.productAnalytics.controller;

import com.example.productAnalytics.util.ApiResponseBuilder;
import com.example.productAnalytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/all")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> all() {
        return analyticsService.all()
                .thenApply(list ->
                        ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is a list of all the products", list)
                );
    }

    @GetMapping("/one")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> one(@RequestParam String productId) {
        return analyticsService.one(productId)
                .thenApply(product ->
                        ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is the product", product)
                );
    }

    @PutMapping("/view")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> view(@RequestParam String productId, @RequestParam String userIp) {
        return analyticsService.view(productId, userIp)
                .thenApply(v ->
                        ApiResponseBuilder.makeResponse(HttpStatus.OK, "Product view increased")
                );
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> recent() {
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is a list of all the recent products", analyticsService.recent());
    }

    @GetMapping("/frequent")
    public ResponseEntity<Map<String, Object>> frequent() {
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is a list of all the frequent products", analyticsService.frequent());
    }
}
