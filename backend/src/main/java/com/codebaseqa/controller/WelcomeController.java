package com.codebaseqa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Codebase QA Backend");
        response.put("status", "running");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.of(
            "health", "/actuator/health",
            "auth", "/api/auth/github",
            "docs", "https://github.com/akash20122001/codebase-qa-full"
        ));
        return response;
    }
}
