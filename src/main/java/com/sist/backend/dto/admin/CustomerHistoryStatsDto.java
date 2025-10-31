package com.sist.backend.dto.admin;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistoryStatsDto {
    // 평균 평점
    private BigDecimal averageRating;
    
    // 피드백 갯수 (리뷰가 작성된 예약 수)
    private Long feedbackCount;
}

