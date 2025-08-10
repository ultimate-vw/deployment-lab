package com.sumitsee.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {
    @GetMapping("/api/versions")
    public String getVersion() {
        return "Hi I am backend B";
    }

}

