package com.sist.backend.dto.admin;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueSummaryDto {
    private Long todayRevenue;          // 오늘 매출 합계(원)
    private Integer todayPayments;      // 오늘 결제 건수
    private List<MonthRevenueDto> monthlyRevenue;   // 월별 매출

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthRevenueDto {
        private LocalDate month; // 해당 월의 1일
        private Long revenue;
    }
}


