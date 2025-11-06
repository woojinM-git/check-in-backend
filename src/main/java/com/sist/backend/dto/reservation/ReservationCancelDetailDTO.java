package com.sist.backend.dto.reservation;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ReservationCancelDetailDTO {

    private String orderNum; // fallback to RES-{reservIdx} if null
    private String hotelTitle;
    private String checkIn;   // yyyy-MM-dd
    private String checkOut;  // yyyy-MM-dd
    private Integer status;   // 1 confirmed, 2 cancelled

    private Integer totalPrice;      // reservation.totalPrice (fallback computed)
    private Integer couponDiscount;  // 0 if not applicable
    private Integer pointsUsed;      // RoomPayment.pointsUsed
    private Integer cashUsed;        // RoomPayment.cashUsed
    private Integer cardPaid;        // RoomPayment.price

    // 환불 예상 정보 (취소 전 계산)
    private Double expectedRefundRate;  // 예상 환불율
    private String expectedRefundMessage;  // 예상 환불 정책 메시지
    private Integer expectedPaymentRefund;  // 예상 결제 환불 금액
    private Integer expectedCashRestore;  // 예상 캐시 복원 금액
    private Integer expectedPointRestore;  // 예상 포인트 복원 금액
    private Integer expectedTotalRefund;  // 예상 총 환불 금액
}
