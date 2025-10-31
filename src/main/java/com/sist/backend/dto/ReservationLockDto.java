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

    // 응답 필드
    private Boolean success;
    private String message;
    private LocalDateTime expireTime;
    private String lockKey;
}
