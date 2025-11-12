package com.sist.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예약 락 요청/응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationLockDto {

    // 요청 필드
    private Integer customerIdx;
    private String contentId;
    private Integer roomId;
    private String checkIn; // 예약 일자
    private String checkOut;
    private String lockId;//락 키 포함 탭ID 포함
    private String sessionId;
    private String tabId;
    private String initialLockAt;

    // 응답 필드
    private Boolean success;
    private String message;
    private LocalDateTime expireTime;
    private String lockKey;
}
