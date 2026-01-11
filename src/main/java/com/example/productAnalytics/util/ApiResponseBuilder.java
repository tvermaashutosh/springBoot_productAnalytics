package com.example.productAnalytics.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponseBuilder {
    public static ResponseEntity<Map<String, Object>> makeResponse(HttpStatus httpStatus, String message) {
        return ResponseEntity.status(httpStatus).body(Map.of("message", message));
    }

    public static ResponseEntity<Map<String, Object>> makeResponse(HttpStatus httpStatus, String message, Object body) {
        return ResponseEntity.status(httpStatus).body(Map.of("message", message, "body", body));
    }
}
