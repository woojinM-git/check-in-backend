package com.sist.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    private String paymentKey;
    private String orderId;
    private Integer amount;
    private String type; // hotel_reservation, used_hotel, dining_reservation
    private Integer customerIdx;

    // 호텔 예약 관련 필드
    private String contentId;
    private Integer roomId;
    private String checkIn;
    private String checkOut;
    private Integer guests;
    private Integer nights;
    private Integer roomPrice;
    private Integer totalPrice;

    // 다이닝 예약 관련 필드
    private Integer diningIdx;
    private String diningDate;
    private String diningTime;

    // 공통 필드
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String specialRequests;
    private String method; // card, cash, point, mixed
    private Integer pointsUsed;
    private Integer cashUsed;

    // 쿠폰 관련 필드
    private Integer couponIdx;
    private Integer couponDiscount; // 쿠폰 할인 금액

    // 중고 호텔 거래 관련 필드
    private Integer usedTradeIdx;
    private Integer usedItemIdx;

    // 예약 락 식별자
    private String lockId;
    private String sessionId;
    private String tabId;
    private String lockInitialAt;
}
