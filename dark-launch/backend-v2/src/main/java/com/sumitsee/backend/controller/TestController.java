package com.sumitsee.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping("/secure")
    public String secure() {
        return "This is a secured endpoint!";
    }
}

