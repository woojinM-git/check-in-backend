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
    private String type; // hotel_reservation, used_hotel
    private Integer customerIdx;
    private String contentId;
    private Integer roomId;
    private String checkIn;
    private String checkOut;
    private Integer guests;
    private Integer nights;
    private Integer roomPrice;
    private Integer totalPrice;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String specialRequests;
    private String method; // card, cash, point, mixed
    private Integer pointsUsed;
    private Integer cashUsed;
}
