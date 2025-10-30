package com.sist.backend.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.RedisLockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentLockController {

    private final RedisLockService redisLockService;

    //    결제 시작전에 락 걸어버리기
    @PostMapping("/lock")
    public ResponseEntity<?> lockRoom(@RequestBody Map<String, String> body) {
        Integer roomId = Integer.parseInt(body.get("roomId"));
        LocalDate checkinDate = LocalDate.parse(body.get("checkIn"));

        boolean locked = redisLockService.tryLock(roomId, checkinDate, 600);
        if (!locked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 다른 사용자가 결제 진행중입니다."));
        }
        return ResponseEntity.ok(Map.of("message", "락 획득 성공"));
    }

    //결제 완료 /취소시 락 헤제 (TTL로 만료 되자만 안정빵)
    @PostMapping("/unlock")
    public ResponseEntity<?> unlockRoom(@RequestBody Map<String, String> body) {
        Integer roomId = Integer.parseInt(body.get("roomId"));
        LocalDate checkinDate = LocalDate.parse(body.get("checkIn"));

        redisLockService.unlock(roomId, checkinDate);
        return ResponseEntity.ok(Map.of("message", "락 해제 완료"));

    }
}
