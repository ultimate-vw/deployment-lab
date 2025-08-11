package com.sumitsee.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class VersionController {

    @Value("${feature.newGreeting:false}")
    private boolean globalNewGreeting;


    @GetMapping("/api/versions")
    public String getVersion() {
        return "Hi! I am backend to test feature toggle feature!";
    }

    @GetMapping("/api/features")
    public String features() {
        return Map.of("newGreeting", globalNewGreeting).toString();
    }

    @GetMapping("/api/whereami")
    public String whereami(@RequestHeader Map<String, String> headers) {
        return Map.of(
                "service", "feature-toggle-demo",
                "version", "v1",
                "headers", headers
        ).toString();
    }

    @GetMapping("/api/greeting")
    public String greeting(@RequestHeader(value = "X-Features", required = false) String featureHeader) {
        boolean perRequestNewGreeting = featureHeader != null && featureHeader.toLowerCase().contains("newgreeting");
        boolean enabled = globalNewGreeting || perRequestNewGreeting;
        String msg = enabled
                ? "✨ Hello from the NEW greeting (flag on)!"
                : "Hello from the classic greeting (flag off).";
        return Map.of(
                "newGreetingEnabled", enabled,
                "message", msg
        ).toString();
    }
}

