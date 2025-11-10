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
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.HotelSettlementRepository;
import com.sist.backend.repository.hotel.HotelInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RoomPaymentRepository roomPaymentRepository;
    private final HotelSettlementRepository hotelSettlementRepository;
    private final HotelInfoRepository hotelInfoRepository;

    public RevenueSummaryDto getRevenueSummary(String contentId, Integer year) {
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
        // year가 지정된 경우 DB에서 해당 연도 데이터만 조회, 아니면 전체 조회
        List<HotelSettlement> settlements;
        if (year != null) {
            settlements = hotelSettlementRepository.findByContentIdAndYear(contentId, String.valueOf(year));
        } else {
            settlements = hotelSettlementRepository.findByContentId(contentId);
        }
        
        Map<YearMonth, Long> monthToRevenue = new HashMap<>();

        for (HotelSettlement settlement : settlements) {
            // settlementMonth 형식: "2024-01"
            String settlementMonth = settlement.getSettlementMonth();
            String[] parts = settlementMonth.split("-");
            if (parts.length == 2) {
                try {
                    int settlementYear = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    YearMonth ym = YearMonth.of(settlementYear, month);
                    // totalRevenue는 총 수익 (정산된 금액)
                    monthToRevenue.put(ym, settlement.getTotalRevenue());
                } catch (NumberFormatException e) {
                    // 잘못된 형식이면 스킵
                    continue;
                }
            }
        }

        List<MonthRevenueDto> monthly = new ArrayList<>();
        
        // 연도가 지정된 경우 해당 연도의 월별 데이터 생성
        YearMonth start;
        int monthCount;
        YearMonth currentYearMonth = YearMonth.now();
        int currentYear = currentYearMonth.getYear();
        int currentMonth = currentYearMonth.getMonthValue();
        
        if (year != null) {
            // HotelInfo의 serviceStartDate 확인
            HotelInfo hotelInfo = hotelInfoRepository.findById(contentId).orElse(null);
            
            if (hotelInfo != null && hotelInfo.getServiceStartDate() != null) {
                LocalDate serviceStartDate = hotelInfo.getServiceStartDate();
                int serviceStartYear = serviceStartDate.getYear();
                int serviceStartMonth = serviceStartDate.getMonthValue();
                
                // 선택된 연도가 서비스 시작 연도인 경우
                if (year == serviceStartYear) {
                    start = YearMonth.of(year, serviceStartMonth);
                    // 현재 연도이면 현재 월까지만, 아니면 12월까지
                    if (year == currentYear) {
                        monthCount = currentMonth - serviceStartMonth + 1;
                    } else {
                        monthCount = 13 - serviceStartMonth;
                    }
                } else {
                    // 이후 연도는 1월부터 표시
                    start = YearMonth.of(year, 1);
                    // 현재 연도이면 현재 월까지만, 아니면 12월까지
                    if (year == currentYear) {
                        monthCount = currentMonth;
                    } else {
                        monthCount = 12;
                    }
                }
            } else {
                // serviceStartDate가 없으면 1월부터 표시
                start = YearMonth.of(year, 1);
                // 현재 연도이면 현재 월까지만, 아니면 12월까지
                if (year == currentYear) {
                    monthCount = currentMonth;
                } else {
                    monthCount = 12;
                }
            }
        } else {
            // year가 없으면 최근 12개월
            start = YearMonth.now().minusMonths(11);
            monthCount = 12;
        }
        
        for (int i = 0; i < monthCount; i++) {
            YearMonth ym = start.plusMonths(i);
            // 현재 월을 초과하지 않도록 체크
            if (ym.isAfter(currentYearMonth)) {
                break;
            }
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
    
    /**
     * 최소 연도 반환 (우선순위: serviceStartDate > HotelSettlement)
     * 1. HotelInfo의 serviceStartDate가 있으면 해당 연도 반환
     * 2. 없으면 HotelSettlement에서 가장 오래된 settlementMonth의 연도 반환
     * 3. 둘 다 없으면 null 반환
     */
    public Integer getMinYear(String contentId) {
        // 1. HotelInfo의 serviceStartDate 확인 (우선순위)
        HotelInfo hotelInfo = hotelInfoRepository.findById(contentId).orElse(null);
        if (hotelInfo != null && hotelInfo.getServiceStartDate() != null) {
            return hotelInfo.getServiceStartDate().getYear();
        }
        
        // 2. HotelSettlement에서 가장 오래된 연도 계산 (fallback)
        List<HotelSettlement> settlements = hotelSettlementRepository.findByContentId(contentId);
        if (settlements.isEmpty()) {
            return null;
        }
        
        Integer minYear = null;
        for (HotelSettlement settlement : settlements) {
            String settlementMonth = settlement.getSettlementMonth();
            String[] parts = settlementMonth.split("-");
            if (parts.length == 2) {
                try {
                    int year = Integer.parseInt(parts[0]);
                    if (minYear == null || year < minYear) {
                        minYear = year;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return minYear;
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


