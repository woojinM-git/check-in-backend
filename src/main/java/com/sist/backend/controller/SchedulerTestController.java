package com.sist.backend.controller;

import com.sist.backend.service.RoomReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 스케줄러 테스트용 컨트롤러
 * 
 * - 개발/테스트 환경에서 스케줄러를 수동으로 실행할 수 있도록 제공
 * - 프로덕션 환경에서는 접근 제한 권장
 */
@RestController
@RequestMapping("/api/test/scheduler")
@RequiredArgsConstructor
public class SchedulerTestController {

    private final RoomReservationService roomReservationService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 체크아웃 시간이 지난 예약을 이용완료로 변경하는 스케줄러 수동 실행
     * 
     * @return 처리된 예약 개수와 결과 메시지
     */
    @PostMapping("/complete-reservations")
    public ResponseEntity<Map<String, Object>> completeExpiredReservations() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int completedCount = roomReservationService.completeExpiredReservations();
            
            response.put("success", true);
            response.put("message", "스케줄러 실행 완료");
            response.put("completedCount", completedCount);
            response.put("description", completedCount + "개의 예약이 이용완료로 변경되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "스케줄러 실행 중 오류 발생");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 서버 시간과 DB 시간 확인
     * 
     * @return 서버 시간, DB 시간, 시간대 정보
     */
    @GetMapping("/time-check")
    public ResponseEntity<Map<String, Object>> checkServerAndDbTime() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 서버 시간 정보
            LocalDateTime serverDateTime = LocalDateTime.now();
            LocalDate serverDate = LocalDate.now();
            LocalTime serverTime = LocalTime.now();
            ZoneId serverZone = ZoneId.systemDefault();
            ZonedDateTime serverZonedDateTime = ZonedDateTime.now(serverZone);
            
            // DB 시간 조회 (MySQL)
            String dbDateTime = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
            String dbDate = jdbcTemplate.queryForObject("SELECT CURDATE()", String.class);
            String dbTime = jdbcTemplate.queryForObject("SELECT CURTIME()", String.class);
            String dbGlobalTimezone = jdbcTemplate.queryForObject("SELECT @@global.time_zone", String.class);
            String dbSessionTimezone = jdbcTemplate.queryForObject("SELECT @@session.time_zone", String.class);
            
            // 응답 데이터 구성
            Map<String, String> serverTimeInfo = new HashMap<>();
            serverTimeInfo.put("dateTime", serverDateTime.toString());
            serverTimeInfo.put("date", serverDate.toString());
            serverTimeInfo.put("time", serverTime.toString());
            serverTimeInfo.put("zoneId", serverZone.toString());
            serverTimeInfo.put("zonedDateTime", serverZonedDateTime.toString());
            serverTimeInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            Map<String, String> dbTimeInfo = new HashMap<>();
            dbTimeInfo.put("dateTime", dbDateTime);
            dbTimeInfo.put("date", dbDate);
            dbTimeInfo.put("time", dbTime);
            dbTimeInfo.put("globalTimezone", dbGlobalTimezone);
            dbTimeInfo.put("sessionTimezone", dbSessionTimezone);
            
            response.put("success", true);
            response.put("serverTime", serverTimeInfo);
            response.put("databaseTime", dbTimeInfo);
            
            // 시간 차이 계산 (밀리초 단위)
            try {
                // DB 시간을 LocalDateTime으로 파싱하여 비교
                LocalDateTime dbLocalDateTime = LocalDateTime.parse(dbDateTime.replace(" ", "T"));
                long diffSeconds = java.time.Duration.between(serverDateTime, dbLocalDateTime).getSeconds();
                
                Map<String, Object> timeDiff = new HashMap<>();
                timeDiff.put("differenceSeconds", diffSeconds);
                timeDiff.put("differenceMinutes", diffSeconds / 60);
                timeDiff.put("isSynchronized", Math.abs(diffSeconds) < 5); // 5초 이내 차이는 동기화된 것으로 간주
                
                response.put("timeDifference", timeDiff);
            } catch (Exception e) {
                response.put("timeDifference", "계산 실패: " + e.getMessage());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "시간 확인 중 오류 발생");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}

