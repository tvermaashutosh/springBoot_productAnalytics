package com.example.productAnalytics.exception;

import com.example.productAnalytics.util.ApiResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Map<Class<? extends Exception>, HttpStatus> map = Map.of(
            Exception.class, HttpStatus.INTERNAL_SERVER_ERROR,
            SecurityException.class, HttpStatus.FORBIDDEN
    );

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception e) {
        HttpStatus status = getStatus(e);
        return ApiResponseBuilder.makeResponse(status, e.toString());
    }

    private static HttpStatus getStatus(Exception e) {
        Class<?> clazz = e.getClass();
        HttpStatus status = null;

        while (clazz != Throwable.class) {
            if (map.containsKey(clazz)) {
                status = map.get(clazz);
                break;
            }
            clazz = clazz.getSuperclass();
        }

        return status;
    }
}
