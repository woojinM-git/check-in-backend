package com.sist.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sist.backend.dto.admin.RevenueSummaryDto;
import com.sist.backend.dto.admin.RevenueSummaryDto.MonthRevenueDto;
import com.sist.backend.dto.admin.DailyRevenueDto;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.HotelSettlement;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.HotelSettlementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RoomPaymentRepository roomPaymentRepository;
    private final HotelSettlementRepository hotelSettlementRepository;

    public RevenueSummaryDto getRevenueSummary(String contentId) {
        // 오늘 매출은 RoomPayment에서 계산 (일별 통계는 제거했지만 오늘 매출은 유지)
        List<RoomPayment> payments = roomPaymentRepository.findAllByContentIdWithReservations(contentId);
        LocalDate today = LocalDate.now();

        long todayRevenue = 0L;
        int todayCount = 0;

        for (RoomPayment rp : payments) {
            long price = rp.getPrice() != null ? rp.getPrice().longValue() : 0L;

            LocalDate refDate = null;
            if (rp.getApprovedAt() != null) {
                refDate = rp.getApprovedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            if (refDate != null && refDate.isEqual(today)) {
                todayRevenue += price;
                todayCount += 1;
            }
        }

        // 월별 매출은 HotelSettlement 테이블에서 가져오기
        List<HotelSettlement> settlements = hotelSettlementRepository.findByContentId(contentId);
        Map<YearMonth, Long> monthToRevenue = new HashMap<>();

        for (HotelSettlement settlement : settlements) {
            // settlementMonth 형식: "2024-01"
            String settlementMonth = settlement.getSettlementMonth();
            String[] parts = settlementMonth.split("-");
            if (parts.length == 2) {
                try {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    YearMonth ym = YearMonth.of(year, month);
                    // totalRevenue는 총 수익 (정산된 금액)
                    monthToRevenue.put(ym, settlement.getTotalRevenue());
                } catch (NumberFormatException e) {
                    // 잘못된 형식이면 스킵
                    continue;
                }
            }
        }

        List<MonthRevenueDto> monthly = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            Long revenue = monthToRevenue.getOrDefault(ym, 0L);
            monthly.add(MonthRevenueDto.builder()
                .month(ym.atDay(1))
                .revenue(revenue)
                .build());
        }

        return RevenueSummaryDto.builder()
            .todayRevenue(todayRevenue)
            .todayPayments(todayCount)
            .monthlyRevenue(monthly)
            .build();
    }

    public List<DailyRevenueDto> getDailyRevenue(String contentId, LocalDate start, LocalDate end) {
        List<RoomPayment> payments = roomPaymentRepository.findAllByContentIdWithReservations(contentId);
        Map<LocalDate, DailyRevenueDto> dayToAgg = new HashMap<>();

        // 초기 구간 생성 (누락일 0 채우기)
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            dayToAgg.put(cur, DailyRevenueDto.builder().day(cur).revenue(0L).payments(0).build());
            cur = cur.plusDays(1);
        }

        for (RoomPayment rp : payments) {
            LocalDate refDate = null;
            if (rp.getApprovedAt() != null) {
                refDate = rp.getApprovedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (refDate == null) continue;
            if (refDate.isBefore(start) || refDate.isAfter(end)) continue;

            long price = rp.getPrice() != null ? rp.getPrice().longValue() : 0L;
            DailyRevenueDto agg = dayToAgg.get(refDate);
            if (agg != null) {
                agg.setRevenue(agg.getRevenue() + price);
                agg.setPayments(agg.getPayments() + 1);
            }
        }

        List<DailyRevenueDto> rows = new ArrayList<>(dayToAgg.values());
        rows.sort((a, b) -> a.getDay().compareTo(b.getDay()));
        return rows;
    }
}


