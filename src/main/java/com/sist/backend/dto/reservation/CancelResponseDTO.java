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
    private Integer refundTotalAmount;
    private Integer refundCash;
    private Integer refundPoint;
    private Integer refundStatus; // 0: 진행중, 1: 완료, 2: 실패
    private String cancelReason;
}
