package com.sist.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistoryDto {
    // 고객 정보
    private String customerName;
    private String customerId;
    private Integer customerIdx;
    
    // 예약 정보
    private Integer reservIdx;
    
    // 객실 정보
    private String roomName;
    
    // 숙박 일정
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    
    // 예약 금액
    private Integer totalPrice;
    
    // 상태 (2: 취소, 4: 완료)
    private Integer status;
    
    // 평점 (Review 정보 - 최근 리뷰)
    private BigDecimal star;  // 평점
    private String reviewContent; // 리뷰 내용
    private LocalDate reviewCreatedAt; // 리뷰 작성일
}

