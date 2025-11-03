package com.sist.backend.dto.admin;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListDto {
    // 고객 기본 정보
    private Integer customerIdx;
    private String id;
    private String name;
    private String email;
    private String phone;
    
    // 예약 통계
    private Long reservationCount;  // 예약 횟수
    private Long totalPaymentAmount; // 총 결제 금액
    private LocalDate lastVisitDate; // 가장 최근 방문
    private String rank; // 등급
    
    // 사용한 적이 있었던 날들의 기록 (체크인 날짜 목록)
    private List<LocalDate> visitedDates;
}

