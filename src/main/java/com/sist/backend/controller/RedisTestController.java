 package com.sist.backend.controller;

 import lombok.RequiredArgsConstructor;
 import org.springframework.data.redis.core.StringRedisTemplate;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.RestController;

 /**
  * Redis 연결 테스트 컨트롤러
  *
  * - Redis에 ping 값을 쓰고 다시 읽어오는 방식으로 정상 동작 확인
  * - 연결 성공 시 "Redis 연결 성공!" 메시지 반환
  */
 @RestController
 @RequiredArgsConstructor
 public class RedisTestController {

     private final StringRedisTemplate redisTemplate;

     @GetMapping("/api/test/redis")
     public String testRedis() {
         try {
             // test:ping 키에 pong 값을 저장
             redisTemplate.opsForValue().set("test:ping", "pong");
             // 다시 가져와서 검증
             String value = redisTemplate.opsForValue().get("test:ping");

             return "✅ Redis 연결 성공! 값: " + value;
         } catch (Exception e) {
             return "❌ Redis 연결 실패: " + e.getMessage();
         }
     }
 }
