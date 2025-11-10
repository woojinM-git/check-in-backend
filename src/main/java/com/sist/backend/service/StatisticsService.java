package com.sist.backend.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.stream.Collectors;

import static com.sist.backend.entity.QCustomer.customer;
import static com.sist.backend.entity.QHotelInfo.hotelInfo;
import static com.sist.backend.entity.QHotelSettlement.hotelSettlement;
import static com.sist.backend.entity.QRegistrationRequest.registrationRequest;
import static com.sist.backend.entity.QRoomReservation.roomReservation;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final JPAQueryFactory queryFactory;
    private final CacheManager cacheManager;

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
     * 전체 총 매출액 조회 (모든 기간)
     * hotelSettlement 테이블의 totalRevenue 합계를 조회
     * 플랫폼을 통한 모든 호텔의 총 거래액을 집계
     * 
     * @return 전체 총 매출액
     */
    public Long getTotalRevenueAll() {
        Long result = queryFactory
            .select(hotelSettlement.totalRevenue.sum())
            .from(hotelSettlement)
            .fetchOne();

        return result != null ? result : 0L;
    }

    /**
     * 총 매출액 조회 (날짜 범위 적용)
     * hotelSettlement 테이블의 totalRevenue 합계를 조회
     * 플랫폼을 통한 모든 호텔의 총 거래액을 집계
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
            .select(hotelSettlement.totalRevenue.sum())
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
     * 지역별 통계 조회 (호텔 수 많은 순 상위 4개)
     * 캐싱 적용: 매일 새벽 6시에 갱신, 하루 동안 캐시 사용
     * 
     * @return 지역별 통계 리스트 (regionName, hotels, reservations, revenue, share)
     */
    public List<Map<String, Object>> getRegionStatistics() {
        // 캐시 확인 (캐시 히트 시 로그 출력)
        Cache cache = cacheManager.getCache("regionStatistics");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get("top4");
            if (wrapper != null && wrapper.get() != null) {
                log.info("✅ [캐시 히트] 지역별 통계 데이터를 캐시에서 가져왔습니다.");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cachedResult = (List<Map<String, Object>>) wrapper.get();
                return cachedResult;
            }
        }
        
        // 캐시 미스 시 DB 조회
        log.info("❌ [캐시 미스] 지역별 통계 조회 시작 - DB에서 데이터를 조회합니다.");
        return getRegionStatisticsFromDb();
    }
    
    /**
     * 지역별 통계 DB 조회 (내부 메서드)
     * @Cacheable이 적용되어 캐시에 자동 저장됨
     */
    @Cacheable(value = "regionStatistics", key = "'top4'")
    private List<Map<String, Object>> getRegionStatisticsFromDb() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        String currentMonth = String.format("%04d-%02d", now.getYear(), now.getMonthValue());
        
        // 1. 지역별 호텔 수 집계 (운영중인 호텔만, areaCode가 있는 것만)
        List<com.querydsl.core.Tuple> hotelCounts = queryFactory
            .select(
                hotelInfo.areaCode,
                hotelInfo.count()
            )
            .from(hotelInfo)
            .where(
                hotelInfo.status.eq(0)
                    .and(hotelInfo.areaCode.isNotNull())
            )
            .groupBy(hotelInfo.areaCode)
            .orderBy(hotelInfo.count().desc())
            .limit(4)
            .fetch();
        
        if (hotelCounts.isEmpty()) {
            log.warn("지역별 통계 데이터가 없습니다.");
            return new ArrayList<>();
        }
        
        // areaCode 리스트 추출
        List<String> areaCodes = hotelCounts.stream()
            .map(t -> t.get(hotelInfo.areaCode))
            .collect(Collectors.toList());
        
        // 2. 지역별 예약 건수 집계 (병렬 처리)
        CompletableFuture<Map<String, Long>> reservationCountsFuture = CompletableFuture.supplyAsync(() -> {
            List<com.querydsl.core.Tuple> reservationData = queryFactory
                .select(
                    hotelInfo.areaCode,
                    roomReservation.count()
                )
                .from(roomReservation)
                .innerJoin(hotelInfo)
                    .on(roomReservation.contentid.eq(hotelInfo.contentId))
                .where(
                    roomReservation.status.eq(4)
                        .and(hotelInfo.areaCode.in(areaCodes))
                        .and(roomReservation.checkoutDate.goe(startOfMonth))
                        .and(roomReservation.checkoutDate.loe(now))
                )
                .groupBy(hotelInfo.areaCode)
                .fetch();
            
            return reservationData.stream()
                .collect(Collectors.toMap(
                    t -> t.get(hotelInfo.areaCode),
                    t -> t.get(roomReservation.count())
                ));
        });
        
        // 3. 지역별 매출 집계 (병렬 처리)
        CompletableFuture<Map<String, Long>> revenuesFuture = CompletableFuture.supplyAsync(() -> {
            // hotelSettlement에서 현재 월 데이터 조회
            List<com.querydsl.core.Tuple> settlementData = queryFactory
                .select(
                    hotelInfo.areaCode,
                    hotelSettlement.totalRevenue.sum()
                )
                .from(hotelSettlement)
                .innerJoin(hotelInfo)
                    .on(hotelSettlement.contentId.eq(hotelInfo.contentId))
                .where(
                    hotelSettlement.settlementMonth.eq(currentMonth)
                        .and(hotelInfo.areaCode.in(areaCodes))
                )
                .groupBy(hotelInfo.areaCode)
                .fetch();
            
            Map<String, Long> revenueMap = settlementData.stream()
                .collect(Collectors.toMap(
                    t -> t.get(hotelInfo.areaCode),
                    t -> t.get(hotelSettlement.totalRevenue.sum()) != null 
                        ? t.get(hotelSettlement.totalRevenue.sum()) 
                        : 0L
                ));
            
            // hotelSettlement에 데이터가 없는 지역은 roomReservation에서 계산
            List<String> missingAreas = areaCodes.stream()
                .filter(areaCode -> !revenueMap.containsKey(areaCode) || revenueMap.get(areaCode) == 0)
                .collect(Collectors.toList());
            
            if (!missingAreas.isEmpty()) {
                List<com.querydsl.core.Tuple> reservationRevenueData = queryFactory
                    .select(
                        hotelInfo.areaCode,
                        roomReservation.totalPrice.sum()
                    )
                    .from(roomReservation)
                    .innerJoin(hotelInfo)
                        .on(roomReservation.contentid.eq(hotelInfo.contentId))
                    .where(
                        roomReservation.status.eq(4)
                            .and(hotelInfo.areaCode.in(missingAreas))
                            .and(roomReservation.checkoutDate.goe(startOfMonth))
                            .and(roomReservation.checkoutDate.loe(now))
                    )
                    .groupBy(hotelInfo.areaCode)
                    .fetch();
                
                for (com.querydsl.core.Tuple tuple : reservationRevenueData) {
                    String areaCode = tuple.get(hotelInfo.areaCode);
                    Integer revenue = tuple.get(roomReservation.totalPrice.sum());
                    revenueMap.put(areaCode, revenue != null ? revenue.longValue() : 0L);
                }
            }
            
            return revenueMap;
        });
        
        // 병렬 처리 완료 대기
        CompletableFuture.allOf(reservationCountsFuture, revenuesFuture).join();
        
        Map<String, Long> reservationCounts;
        Map<String, Long> revenues;
        try {
            reservationCounts = reservationCountsFuture.get();
            revenues = revenuesFuture.get();
        } catch (Exception e) {
            log.error("지역별 통계 조회 중 오류 발생", e);
            throw new RuntimeException("지역별 통계 조회 실패", e);
        }
        
        // 전체 예약 건수 계산 (비율 계산용)
        Long totalReservations = reservationCounts.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        
        // 4. 결과 조합
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = 0; i < hotelCounts.size(); i++) {
            com.querydsl.core.Tuple tuple = hotelCounts.get(i);
            String areaCode = tuple.get(hotelInfo.areaCode);
            Long hotelCount = tuple.get(hotelInfo.count());
            
            Long reservationCount = reservationCounts.getOrDefault(areaCode, 0L);
            Long revenue = revenues.getOrDefault(areaCode, 0L);
            
            // 비율 계산
            double reservationShare = totalReservations > 0 
                ? (double) reservationCount / totalReservations * 100 
                : 0.0;
            
            // 지역명 매핑
            String regionName = getRegionName(areaCode);
            
            Map<String, Object> regionMap = new HashMap<>();
            regionMap.put("region", regionName);
            regionMap.put("hotels", hotelCount);
            regionMap.put("reservations", reservationCount);
            regionMap.put("revenue", revenue);
            regionMap.put("share", String.format("%.0f%%", reservationShare));
            
            result.add(regionMap);
        }
        
        log.info("✅ 지역별 통계 조회 완료: {}개 지역 (캐시에 저장됨)", result.size());
        return result;
    }
    
    /**
     * 지역별 통계 캐시 삭제
     */
    @CacheEvict(value = "regionStatistics", key = "'top4'")
    public void evictRegionStatisticsCache() {
        log.info("지역별 통계 캐시 삭제 완료");
    }
    
    /**
     * 지역별 통계 캐시 강제 갱신 (스케줄러에서 호출)
     * 캐시를 삭제한 후 새로 생성
     */
    public void refreshRegionStatisticsCache() {
        log.info("지역별 통계 캐시 갱신 시작");
        // 1. 기존 캐시 삭제
        evictRegionStatisticsCache();
        // 2. 새 캐시 생성 (메서드 호출 시 @Cacheable이 자동으로 캐시에 저장)
        getRegionStatisticsFromDb();
        log.info("지역별 통계 캐시 갱신 완료");
    }
    
    /**
     * areaCode를 지역명으로 변환
     */
    private String getRegionName(String areaCode) {
        if (areaCode == null) {
            return "미지정";
        }
        
        Map<String, String> areaCodeMap = new HashMap<>();
        areaCodeMap.put("1", "서울");
        areaCodeMap.put("2", "인천");
        areaCodeMap.put("3", "대전");
        areaCodeMap.put("4", "대구");
        areaCodeMap.put("5", "광주");
        areaCodeMap.put("6", "부산");
        areaCodeMap.put("7", "울산");
        areaCodeMap.put("8", "세종");
        areaCodeMap.put("31", "경기");
        areaCodeMap.put("32", "강원");
        areaCodeMap.put("33", "충북");
        areaCodeMap.put("34", "충남");
        areaCodeMap.put("35", "경북");
        areaCodeMap.put("36", "경남");
        areaCodeMap.put("37", "전북");
        areaCodeMap.put("38", "전남");
        areaCodeMap.put("39", "제주");
        
        return areaCodeMap.getOrDefault(areaCode, "기타");
    }
    
    /**
     * 회원 등급별 통계 조회
     * 각 등급별 인원수, 비율, 평균 지출액 집계
     * 
     * @return 회원 등급별 통계 리스트 (grade, count, percentage, avgSpending)
     */
    public List<Map<String, Object>> getMemberGradeStatistics() {
        // 등급 목록 (우선순위 순서)
        List<String> grades = List.of("Explorer", "First Class", "Sky Suite", "Traveler", "VIP");
        
        // 전체 활성 회원 수 조회 (status = 0)
        Long totalMembers = queryFactory
            .select(customer.count())
            .from(customer)
            .where(customer.status.eq(0))
            .fetchOne();
        
        if (totalMembers == null || totalMembers == 0) {
            log.warn("활성 회원이 없습니다.");
            return new ArrayList<>();
        }
        
        // 등급별 통계 집계
        List<com.querydsl.core.Tuple> gradeStats = queryFactory
            .select(
                customer.rank,
                customer.count(),
                customer.totalPrice.avg()
            )
            .from(customer)
            .where(
                customer.status.eq(0)
                    .and(customer.rank.in(grades))
            )
            .groupBy(customer.rank)
            .fetch();
        
        // 등급별 통계를 Map으로 변환
        Map<String, Map<String, Object>> gradeMap = new HashMap<>();
        for (com.querydsl.core.Tuple tuple : gradeStats) {
            String rank = tuple.get(customer.rank);
            Long count = tuple.get(customer.count());
            Double avgSpending = tuple.get(customer.totalPrice.avg());
            
            Map<String, Object> stat = new HashMap<>();
            stat.put("grade", rank);
            stat.put("count", count);
            stat.put("avgSpending", avgSpending != null ? avgSpending.longValue() : 0L);
            stat.put("percentage", 0.0); // 나중에 계산
            
            gradeMap.put(rank, stat);
        }
        
        // 결과 리스트 생성 (지정된 순서대로)
        List<Map<String, Object>> result = new ArrayList<>();
        for (String grade : grades) {
            Map<String, Object> stat = gradeMap.get(grade);
            if (stat != null) {
                // 비율 계산
                Long count = (Long) stat.get("count");
                double percentage = (double) count / totalMembers * 100;
                stat.put("percentage", String.format("%.0f%%", percentage));
                
                // 평균 지출액 포맷팅
                Long avgSpending = (Long) stat.get("avgSpending");
                stat.put("avgSpending", avgSpending);
                
                result.add(stat);
            } else {
                // 해당 등급에 회원이 없는 경우
                Map<String, Object> emptyStat = new HashMap<>();
                emptyStat.put("grade", grade);
                emptyStat.put("count", 0L);
                emptyStat.put("avgSpending", 0L);
                emptyStat.put("percentage", "0%");
                result.add(emptyStat);
            }
        }
        
        log.info("회원 등급별 통계 조회 완료: {}개 등급", result.size());
        return result;
    }
    
    /**
     * 호텔별 매출 순위 조회 (최근 30일 기준)
     * hotelSettlement 테이블을 우선 사용하고, 없으면 roomReservation에서 직접 집계
     * 
     * @param limit 상위 N개 호텔 (기본값: 4)
     * @return 호텔별 매출 순위 리스트 (rank, name, revenue, reservations)
     */
    public List<Map<String, Object>> getHotelRevenueRankings(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 4; // 기본값: 상위 4개
        }
        
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusDays(30); // 오늘로부터 30일 전
        
        // 최근 30일 범위에 포함되는 모든 월의 settlementMonth 리스트 생성
        List<String> settlementMonths = generateSettlementMonths(startDate, now);
        
        // 1. hotelSettlement에서 최근 30일 데이터 조회 시도
        List<com.querydsl.core.Tuple> settlementRankings = new ArrayList<>();
        if (!settlementMonths.isEmpty()) {
            settlementRankings = queryFactory
                .select(
                    hotelSettlement.contentId,
                    hotelInfo.title,
                    hotelSettlement.totalRevenue.sum()
                )
                .from(hotelSettlement)
                .innerJoin(hotelInfo)
                    .on(hotelSettlement.contentId.eq(hotelInfo.contentId))
                .where(
                    hotelSettlement.settlementMonth.in(settlementMonths)
                        .and(hotelInfo.status.eq(0)) // 운영중인 호텔만
                )
                .groupBy(hotelSettlement.contentId, hotelInfo.title)
                .orderBy(hotelSettlement.totalRevenue.sum().desc())
                .limit(limit)
                .fetch();
        }
        
        List<com.querydsl.core.Tuple> revenueRankings;
        boolean useSettlement = !settlementRankings.isEmpty();
        
        if (useSettlement) {
            // hotelSettlement 데이터 사용
            revenueRankings = settlementRankings;
            log.info("hotelSettlement 테이블에서 호텔별 매출 순위 조회 (최근 30일: {} ~ {})", startDate, now);
        } else {
            // hotelSettlement에 데이터가 없으면 roomReservation에서 직접 집계
            log.info("hotelSettlement에 데이터가 없어 roomReservation에서 직접 집계합니다 (최근 30일: {} ~ {})", startDate, now);
            revenueRankings = queryFactory
                .select(
                    roomReservation.contentid,
                    hotelInfo.title,
                    roomReservation.totalPrice.sum()
                )
                .from(roomReservation)
                .innerJoin(hotelInfo)
                    .on(roomReservation.contentid.eq(hotelInfo.contentId))
                .where(
                    roomReservation.status.eq(4) // 이용 완료된 예약만
                        .and(hotelInfo.status.eq(0)) // 운영중인 호텔만
                        .and(roomReservation.checkoutDate.goe(startDate))
                        .and(roomReservation.checkoutDate.loe(now))
                )
                .groupBy(roomReservation.contentid, hotelInfo.title)
                .orderBy(roomReservation.totalPrice.sum().desc())
                .limit(limit)
                .fetch();
        }
        
        if (revenueRankings.isEmpty()) {
            log.warn("최근 30일({} ~ {})의 호텔별 매출 데이터가 없습니다.", startDate, now);
            return new ArrayList<>();
        }
        
        // contentId 리스트 추출
        List<String> contentIds = revenueRankings.stream()
            .map(t -> useSettlement 
                ? t.get(hotelSettlement.contentId) 
                : t.get(roomReservation.contentid))
            .collect(Collectors.toList());
        
        // 2. 각 호텔의 예약 건수 집계 (최근 30일 기준, status=4)
        List<com.querydsl.core.Tuple> reservationCounts = queryFactory
            .select(
                roomReservation.contentid,
                roomReservation.count()
            )
            .from(roomReservation)
            .where(
                roomReservation.status.eq(4)
                    .and(roomReservation.contentid.in(contentIds))
                    .and(roomReservation.checkoutDate.goe(startDate))
                    .and(roomReservation.checkoutDate.loe(now))
            )
            .groupBy(roomReservation.contentid)
            .fetch();
        
        // 예약 건수 Map 생성
        Map<String, Long> reservationCountMap = reservationCounts.stream()
            .collect(Collectors.toMap(
                t -> t.get(roomReservation.contentid),
                t -> t.get(roomReservation.count())
            ));
        
        // 3. 결과 조합
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        
        for (com.querydsl.core.Tuple tuple : revenueRankings) {
            String contentId = useSettlement 
                ? tuple.get(hotelSettlement.contentId) 
                : tuple.get(roomReservation.contentid);
            String hotelName = tuple.get(hotelInfo.title);
            Long totalRevenue = useSettlement 
                ? tuple.get(hotelSettlement.totalRevenue.sum()) 
                : (tuple.get(roomReservation.totalPrice.sum()) != null 
                    ? tuple.get(roomReservation.totalPrice.sum()).longValue() 
                    : 0L);
            Long reservationCount = reservationCountMap.getOrDefault(contentId, 0L);
            
            Map<String, Object> hotelRanking = new HashMap<>();
            hotelRanking.put("rank", rank++);
            hotelRanking.put("name", hotelName != null ? hotelName : contentId);
            hotelRanking.put("revenue", totalRevenue != null ? totalRevenue : 0L);
            hotelRanking.put("reservations", reservationCount);
            
            result.add(hotelRanking);
        }
        
        log.info("호텔별 매출 순위 조회 완료: {}개 호텔 (최근 30일: {} ~ {}, 데이터 소스: {})", 
            result.size(), startDate, now, useSettlement ? "hotelSettlement" : "roomReservation");
        return result;
    }
}


