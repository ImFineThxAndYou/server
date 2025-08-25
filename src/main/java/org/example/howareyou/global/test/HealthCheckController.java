package org.example.howareyou.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/health")
public class HealthCheckController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> checkRedisHealth() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Redis 연결 테스트
            String testKey = "health:test:" + System.currentTimeMillis();
            String testValue = "test";
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            if (testValue.equals(retrievedValue)) {
                result.put("status", "healthy");
                result.put("message", "Redis connection is working properly");
                result.put("timestamp", System.currentTimeMillis());
                log.info("Redis health check passed");
                return ResponseEntity.ok(result);
            } else {
                result.put("status", "unhealthy");
                result.put("message", "Redis read/write test failed");
                result.put("timestamp", System.currentTimeMillis());
                log.error("Redis health check failed: read/write mismatch");
                return ResponseEntity.status(503).body(result);
            }
            
        } catch (Exception e) {
            result.put("status", "unhealthy");
            result.put("message", "Redis connection failed: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            log.error("Redis health check failed", e);
            return ResponseEntity.status(503).body(result);
        }
    }


} 