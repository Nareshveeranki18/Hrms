package com.hrms.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/test")
@CrossOrigin(originPatterns = "*")
public class RedisTestController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/redis-checkin")
    public ResponseEntity<String> testRedis() {
        String key = "attendance:checkin:EMP001";
        String value = "Checked in at: " + Instant.now().toString();
        
        // Save to Redis with a 24-hour (86400 seconds) Time-To-Live (TTL)
        redisTemplate.opsForValue().set(key, value, 86400, TimeUnit.SECONDS);
        
        return ResponseEntity.ok("Success! Event cached in Redis for 24 hours.");
    }
}