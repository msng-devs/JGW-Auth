package com.jaramgroupware.jgwauth.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    @GetMapping("/ping")
    public ResponseEntity<String> ping(){
        return ResponseEntity.ok("PONG");
    }
}
