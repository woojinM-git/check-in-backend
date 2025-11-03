package com.sist.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsDto {
    // 우리 호텔을 이용한 기록이 있는 사용자들의 수
    private Long totalCustomers;
    
    // 이번달 새로 이용을 시작한 사람의 수
    private Long newCustomersThisMonth;
    
    // 평균 호텔 결제 금액
    private Double averagePaymentAmount;
}

