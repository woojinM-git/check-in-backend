package com.sist.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 중고거래 결제 락 요청/응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsedPaymentLockDto {

    // 요청 필드
    private Integer usedTradeIdx;
    private String paymentKey;
    private String orderId;
    private Integer buyerIdx;

    // 응답 필드
    private Boolean success;
    private String message;
    private LocalDateTime expireTime;
    private String lockKey;
}

