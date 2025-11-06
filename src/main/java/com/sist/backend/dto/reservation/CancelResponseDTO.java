package com.sist.backend.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelResponseDTO {

    private String message;
    private Integer reservIdx;
    private double refundRate;
    private String refundMessage; // 환불 정책 메시지
    private Integer refundTotalAmount; // 총 환불 금액
    private Integer paymentRefund; // 실 결제 환불 금액
    private Integer refundCash; // 캐시 복원 금액
    private Integer refundPoint; // 포인트 복원 금액
    private Integer refundStatus; // 0: 진행중, 1: 완료, 2: 실패
    private String cancelReason;
}
