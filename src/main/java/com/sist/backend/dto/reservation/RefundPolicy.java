package com.sist.backend.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundPolicy {
    FULL(1.0, "체크인 7일 전및 당일 예약 전액 환불"),
    HALF(0.5, "체크인 3~6일 전 50% 환불"),
    PARTIAL(0.3, "체크인 1~2일 전 30% 환불"),
    NONE(0.0, "체크인 당일시 환불 불가");

    private final double refundRate;
    private final String message;

    public static RefundPolicy fromRefundRate(double refundRate) {
        if (refundRate >= 1.0) {
            return FULL;
        } else if (refundRate >= 0.5) {
            return HALF;
        } else if (refundRate >= 0.3) {
            return PARTIAL;
        } else {
            return NONE;
        }
    }
}
