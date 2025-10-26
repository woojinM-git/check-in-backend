package com.sist.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Boolean success;
    private String message;
    private String orderId;
    private String paymentKey;
    private Integer amount;
    private String status;
    private LocalDateTime approvedAt;
    private String receiptUrl;
    private String qrUrl;
    private Boolean emailSent;
}
