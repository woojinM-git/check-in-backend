package com.sist.backend.dto.admin;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRevenueDto {
    private LocalDate day;     // 날짜 (YYYY-MM-DD)
    private Long revenue;      // 해당 일 매출 합계
    private Integer payments;  // 해당 일 결제 건수
}


