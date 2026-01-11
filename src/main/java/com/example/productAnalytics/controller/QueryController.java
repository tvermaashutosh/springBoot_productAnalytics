package com.example.productAnalytics.controller;

import com.example.productAnalytics.service.QueryService;
import com.example.productAnalytics.util.ApiResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {
    private final QueryService queryService;

    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestParam("prompt") String prompt) {
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is the sql query", queryService.generate(prompt));
    }

    @GetMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestParam("query") String query) {
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is the sql query result", queryService.execute(query));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history() {
        return ApiResponseBuilder.makeResponse(HttpStatus.OK, "Here is a list of all past queries", queryService.history());
    }
}
