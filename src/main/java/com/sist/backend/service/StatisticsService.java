package com.sist.backend.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.sist.backend.entity.QCustomer.customer;
import static com.sist.backend.entity.QHotelInfo.hotelInfo;
import static com.sist.backend.entity.QHotelSettlement.hotelSettlement;
import static com.sist.backend.entity.QRegistrationRequest.registrationRequest;
import static com.sist.backend.entity.QReview.review;
import static com.sist.backend.entity.QRoomReservation.roomReservation;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final JPAQueryFactory queryFactory;

    /**
     * 날짜 범위에 따른 통계 데이터 조회 (병렬 처리)
     * 
     * @param dateRange "week"(7일), "month"(30일), "quarter"(3개월), "year"(1년)
     * @return 통계 데이터 Map
     */
    public Map<String, Object> getStatistics(String dateRange) {
        LocalDate startDate = calculateStartDate(dateRange);
        LocalDate endDate = LocalDate.now();

        // 병렬 처리로 모든 통계 데이터 조회
        CompletableFuture<Long> totalRevenueFuture = CompletableFuture.supplyAsync(
            () -> getTotalRevenue(startDate, endDate)
        );
        CompletableFuture<Long> totalReservationCountFuture = CompletableFuture.supplyAsync(
            () -> getTotalReservationCount(startDate, endDate)
        );
        CompletableFuture<Long> activeHotelsFuture = CompletableFuture.supplyAsync(
            () -> getActiveHotelsCount()
        );
        CompletableFuture<Long> newHotelsThisMonthFuture = CompletableFuture.supplyAsync(
            () -> getNewHotelsThisMonthCount()
        );
        CompletableFuture<Long> newCustomersThisMonthFuture = CompletableFuture.supplyAsync(
            () -> getNewCustomersThisMonthCount()
        );

        // 모든 Future 완료 대기
        CompletableFuture.allOf(
            totalRevenueFuture,
            totalReservationCountFuture,
            activeHotelsFuture,
            newHotelsThisMonthFuture,
            newCustomersThisMonthFuture
        ).join();

        Map<String, Object> result = new HashMap<>();
        try {
            result.put("totalRevenue", totalRevenueFuture.get());
            result.put("totalReservationCount", totalReservationCountFuture.get());
            result.put("activeHotels", activeHotelsFuture.get());
            result.put("newHotelsThisMonth", newHotelsThisMonthFuture.get());
            result.put("newCustomersThisMonth", newCustomersThisMonthFuture.get());
        } catch (Exception e) {
            log.error("통계 데이터 조회 중 오류 발생", e);
            throw new RuntimeException("통계 데이터 조회 실패", e);
        }

        return result;
    }

    /**
     * 날짜 범위에 따른 시작 날짜 계산
     */
    private LocalDate calculateStartDate(String dateRange) {
        LocalDate now = LocalDate.now();
        return switch (dateRange) {
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            case "quarter" -> now.minusMonths(3);
            case "year" -> now.minusYears(1);
            default -> now.minusDays(30); // 기본값: 30일
        };
    }

    /**
     * 총 매출액 조회 (날짜 범위 적용)
     * hotelSettlement 테이블의 commissionAmount 합계를 조회
     * settlementMonth가 날짜 범위에 포함되는 정산 데이터를 합산
     */
    private Long getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        // 날짜 범위에 포함되는 모든 월의 settlementMonth 리스트 생성
        // 예: startDate=2024-10-15, endDate=2024-12-01 -> ["2024-10", "2024-11", "2024-12"]
        List<String> settlementMonths = generateSettlementMonths(startDate, endDate);
        
        if (settlementMonths.isEmpty()) {
            return 0L;
        }

        Long result = queryFactory
            .select(hotelSettlement.commissionAmount.sum())
            .from(hotelSettlement)
            .where(hotelSettlement.settlementMonth.in(settlementMonths))
            .fetchOne();

        return result != null ? result : 0L;
    }

    /**
     * 날짜 범위에 포함되는 모든 settlementMonth 리스트 생성
     * settlementMonth 형식: "YYYY-MM"
     */
    private List<String> generateSettlementMonths(LocalDate startDate, LocalDate endDate) {
        List<String> months = new ArrayList<>();
        
        // 시작 월부터 종료 월까지 모든 월 추가
        LocalDate current = startDate.withDayOfMonth(1); // 시작 월의 첫 날
        LocalDate endMonth = endDate.withDayOfMonth(1); // 종료 월의 첫 날
        
        while (!current.isAfter(endMonth)) {
            String settlementMonth = String.format("%04d-%02d", current.getYear(), current.getMonthValue());
            months.add(settlementMonth);
            current = current.plusMonths(1);
        }
        
        return months;
    }

    /**
     * 총 예약수 조회 (날짜 범위 적용)
     */
    private Long getTotalReservationCount(LocalDate startDate, LocalDate endDate) {
        Long result = queryFactory
            .select(roomReservation.count())
            .from(roomReservation)
            .where(
                roomReservation.status.eq(4)
                    .and(roomReservation.checkoutDate.goe(startDate))
                    .and(roomReservation.checkoutDate.loe(endDate))
            )
            .fetchOne();

        return result != null ? result : 0L;
    }

    /**
     * 운영중인 호텔 수 조회
     * status = 0: 운영중인 호텔 (등록됨)
     * status = 1: 비활성화된 호텔
     */
    private Long getActiveHotelsCount() {
        Long result = queryFactory
            .select(hotelInfo.count())
            .from(hotelInfo)
            .where(hotelInfo.status.eq(0))
            .fetchOne();

        return result != null ? result : 0L;
    }

    /**
     * 이번달 신규 호텔 수 조회
     */
    private Long getNewHotelsThisMonthCount() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endOfMonthDateTime = now.atTime(23, 59, 59);

        // RegistrationRequest에서 이번달에 승인된 호텔 수 조회
        // status=1은 승인됨을 의미
        Long result = queryFactory
            .select(registrationRequest.count())
            .from(registrationRequest)
            .where(
                registrationRequest.status.eq(1)
                    .and(registrationRequest.approvDate.isNotNull())
                    .and(registrationRequest.approvDate.goe(startOfMonthDateTime))
                    .and(registrationRequest.approvDate.loe(endOfMonthDateTime))
            )
            .fetchOne();

        return result != null ? result : 0L;
    }

    /**
     * 이번달 신규 가입 회원 수 조회
     */
    private Long getNewCustomersThisMonthCount() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endOfMonthDateTime = now.atTime(23, 59, 59);

        Long result = queryFactory
            .select(customer.count())
            .from(customer)
            .where(
                customer.status.eq(0)
                    .and(customer.joinDate.goe(startOfMonthDateTime))
                    .and(customer.joinDate.loe(endOfMonthDateTime))
            )
            .fetchOne();

        return result != null ? result : 0L;
    }

    /**
     * 월별 수수료 수익 조회 (최근 12개월)
     * 마스터 화면의 차트용 데이터
     */
    public List<Map<String, Object>> getMonthlyCommissionRevenue() {
        // 최근 12개월 데이터 조회
        YearMonth now = YearMonth.now();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = 11; i >= 0; i--) {
            YearMonth targetMonth = now.minusMonths(i);
            String settlementMonth = String.format("%04d-%02d", targetMonth.getYear(), targetMonth.getMonthValue());
            
            Long commissionAmount = queryFactory
                .select(hotelSettlement.commissionAmount.sum())
                .from(hotelSettlement)
                .where(hotelSettlement.settlementMonth.eq(settlementMonth))
                .fetchOne();
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", targetMonth.atDay(1)); // LocalDate로 변환
            monthData.put("revenue", commissionAmount != null ? commissionAmount : 0L);
            
            result.add(monthData);
        }
        
        return result;
    }

    /**
     * 호텔별 매출 순위 조회 (상위 4개)
     * 예약건수가 많은 순서로 정렬
     * hotelInfo.count 컬럼 사용
     * 
     * @return 호텔별 매출 순위 리스트 (rank, name, reservations, rating, revenue, growth)
     */
    public List<Map<String, Object>> getHotelRevenueRanking() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        String currentMonth = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        
        // 1. hotelInfo.count가 많은 호텔 상위 4개 조회 (운영중인 호텔만)
        List<com.querydsl.core.Tuple> hotelData = queryFactory
            .select(
                hotelInfo.contentId,
                hotelInfo.title,
                hotelInfo.count
            )
            .from(hotelInfo)
            .where(hotelInfo.status.eq(0)) // 운영중인 호텔만
            .orderBy(hotelInfo.count.desc().nullsLast())
            .limit(4)
            .fetch();
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = 0; i < hotelData.size(); i++) {
            com.querydsl.core.Tuple tuple = hotelData.get(i);
            String contentId = tuple.get(hotelInfo.contentId);
            String title = tuple.get(hotelInfo.title);
            Integer reservationCount = tuple.get(hotelInfo.count);
            
            // count가 null이면 0으로 처리
            if (reservationCount == null) {
                reservationCount = 0;
            }
            
            // 2. 별점 평균 계산 (review 테이블)
            Double avgRating = queryFactory
                .select(review.star.avg())
                .from(review)
                .where(
                    review.contentid.eq(contentId)
                        .and(review.status.eq(false))
                        .and(review.hide.eq(false))
                )
                .fetchOne();
            
            // 3. 현재 월 매출 계산 (hotelSettlement 또는 roomReservation)
            // hotelSettlement에서 현재 월 데이터가 있으면 사용, 없으면 roomReservation에서 계산
            Long monthlyRevenue = queryFactory
                .select(hotelSettlement.totalRevenue.sum())
                .from(hotelSettlement)
                .where(
                    hotelSettlement.contentId.eq(contentId)
                        .and(hotelSettlement.settlementMonth.eq(currentMonth))
                )
                .fetchOne();
            
            // hotelSettlement에 데이터가 없으면 roomReservation에서 계산
            if (monthlyRevenue == null || monthlyRevenue == 0) {
                Integer revenue = queryFactory
                    .select(roomReservation.totalPrice.sum())
                    .from(roomReservation)
                    .where(
                        roomReservation.contentid.eq(contentId)
                            .and(roomReservation.status.eq(4))
                            .and(roomReservation.checkoutDate.goe(startOfMonth))
                            .and(roomReservation.checkoutDate.loe(now))
                    )
                    .fetchOne();
                
                monthlyRevenue = revenue != null ? revenue.longValue() : 0L;
            }
            
            // 4. 전월 대비 증감률 계산 (간단히 0%로 설정, 필요시 추가 구현)
            String growth = "+0%"; // TODO: 전월 데이터와 비교하여 계산
            
            Map<String, Object> hotelMap = new HashMap<>();
            hotelMap.put("rank", i + 1);
            hotelMap.put("name", title != null ? title : "");
            hotelMap.put("reservations", reservationCount.longValue());
            hotelMap.put("rating", avgRating != null ? avgRating : 0.0);
            hotelMap.put("revenue", monthlyRevenue);
            hotelMap.put("growth", growth);
            
            result.add(hotelMap);
        }
        
        return result;
    }
}

