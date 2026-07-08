package com.example.product_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String health() {
        return "Product Service is Running Successfully!";
    }

    @GetMapping("/health")
    public String checkHealth() {
        return "UP";
    }
}