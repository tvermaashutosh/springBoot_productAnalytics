package com.example.productAnalytics.controller;

import com.example.productAnalytics.service.CacheService;
import com.example.productAnalytics.util.ApiResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {
    private final CacheService cacheService;

    @PutMapping("/eviction")
    public ResponseEntity<Map<String, Object>> evictionStrategy(@RequestParam("strategy") String strategy) {
        cacheService.evictionStrategy(strategy);
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Eviction strategy successfully set");
    }

    @PutMapping("/write")
    public ResponseEntity<Map<String, Object>> writeStrategy(@RequestParam("strategy") String strategy) {
        cacheService.writeStrategy(strategy);
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Write strategy successfully set");
    }

    @PutMapping("/parallelWriteThrough")
    public ResponseEntity<Map<String, Object>> parallelWriteThrough(@RequestParam("yes") Boolean yes) {
        cacheService.parallelWriteThrough(yes);
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Parallel write through successfully set");
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here are the cache stats", cacheService.stats());
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clear(@RequestParam("cacheName") String cacheName) {
        cacheService.clear(cacheName);
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, cacheName + " cache cleared");
    }
}
