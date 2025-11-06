package com.sist.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.sist.backend.entity.Dining;
import com.sist.backend.repository.DiningRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 다이닝 정원 관리 서비스 (Redis Atomic Counter 기반)
 * 
 * 역할:
 * - 다이닝 시간대별 정원 관리 (최대 50명)
 * - Redis INCRBY를 통한 원자적 정원 관리
 * - 예약 날짜 기준 동적 TTL 설정 (예약 날짜 이후 1일 후 만료)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiningCapacityService {

    private final StringRedisTemplate redisTemplate;
    private final DiningRepository diningRepository;
    private static final String CAPACITY_PREFIX = "capacity:dining:";
    private static final long CAPACITY_TTL_DAYS_AFTER_RESERVATION = 1L; // 예약 날짜 이후 1일 후 만료

    /**
     * 정원 체크 및 예약 인원 증가 (원자적 연산)
     * 
     * @param diningIdx 다이닝 ID
     * @param reservationDate 예약 날짜
     * @param reservationTime 예약 시간
     * @param guestCount 예약 인원 수
     * @return 정원 체크 및 예약 성공 여부
     * @throws RuntimeException 정원 초과 시 예외 발생
     */
    public void checkAndReserveCapacity(Integer diningIdx, LocalDate reservationDate, LocalTime reservationTime, Integer guestCount) {
        // 다이닝 정보 조회
        Dining dining = diningRepository.findById(diningIdx)
                .orElseThrow(() -> new RuntimeException("다이닝 정보를 찾을 수 없습니다: diningIdx=" + diningIdx));

        Integer maxGuestsPerSlot = dining.getMaxGuestsPerSlot();
        if (maxGuestsPerSlot == null || maxGuestsPerSlot <= 0) {
            throw new RuntimeException("다이닝 정원 정보가 올바르지 않습니다: diningIdx=" + diningIdx);
        }

        String capacityKey = CAPACITY_PREFIX + diningIdx + ":" + reservationDate + ":" + reservationTime;

        try {
            // 현재 예약 인원 조회 (없으면 0)
            String currentCountStr = redisTemplate.opsForValue().get(capacityKey);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

            // 정원 체크
            if (currentCount + guestCount > maxGuestsPerSlot) {
                log.warn("다이닝 정원 초과: diningIdx={}, date={}, time={}, current={}, requested={}, max={}", 
                        diningIdx, reservationDate, reservationTime, currentCount, guestCount, maxGuestsPerSlot);
                throw new RuntimeException(
                        String.format("해당 시간대의 정원이 초과되었습니다. (현재: %d명, 요청: %d명, 최대: %d명)", 
                                currentCount, guestCount, maxGuestsPerSlot)
                );
            }

            // 원자적 연산으로 인원 증가
            Long newCount = redisTemplate.opsForValue().increment(capacityKey, guestCount);
            
            // TTL 설정 (최초 생성 시에만)
            // 예약 날짜 이후 1일 후 만료 (예약 날짜가 지나면 더 이상 필요 없음)
            if (currentCountStr == null) {
                LocalDate today = LocalDate.now();
                long daysUntilReservation = java.time.temporal.ChronoUnit.DAYS.between(today, reservationDate);
                
                // 예약 날짜가 과거면 즉시 만료 (TTL 0 = 삭제)
                if (daysUntilReservation < 0) {
                    redisTemplate.expire(capacityKey, 0, TimeUnit.SECONDS);
                    log.warn("과거 날짜 예약 시도: date={}, 키 즉시 만료", reservationDate);
                } 
                // 예약 날짜가 미래면 (예약 날짜 - 오늘) + 1일 후 만료
                else {
                    long ttlDays = daysUntilReservation + CAPACITY_TTL_DAYS_AFTER_RESERVATION;
                    redisTemplate.expire(capacityKey, ttlDays, TimeUnit.DAYS);
                    log.debug("다이닝 정원 키 TTL 설정: date={}, TTL={}일", reservationDate, ttlDays);
                }
            }

            log.info("다이닝 정원 예약 성공: diningIdx={}, date={}, time={}, before={}, after={}, max={}", 
                    diningIdx, reservationDate, reservationTime, currentCount, newCount, maxGuestsPerSlot);

        } catch (NumberFormatException e) {
            log.error("정원 카운터 파싱 실패: capacityKey={}", capacityKey, e);
            throw new RuntimeException("정원 관리 중 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            // 정원 초과 예외는 그대로 전달
            throw e;
        } catch (Exception e) {
            log.error("다이닝 정원 체크 중 예외 발생: diningIdx={}, date={}, time={}, error={}", 
                    diningIdx, reservationDate, reservationTime, e.getMessage(), e);
            throw new RuntimeException("정원 관리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 예약 취소 시 정원 해제 (인원 감소)
     * 
     * @param diningIdx 다이닝 ID
     * @param reservationDate 예약 날짜
     * @param reservationTime 예약 시간
     * @param guestCount 취소 인원 수
     */
    public void releaseCapacity(Integer diningIdx, LocalDate reservationDate, LocalTime reservationTime, Integer guestCount) {
        String capacityKey = CAPACITY_PREFIX + diningIdx + ":" + reservationDate + ":" + reservationTime;

        try {
            // 원자적 연산으로 인원 감소
            Long newCount = redisTemplate.opsForValue().decrement(capacityKey, guestCount);
            
            // 카운터가 0 이하가 되면 삭제 (정리)
            if (newCount != null && newCount <= 0) {
                redisTemplate.delete(capacityKey);
                log.info("다이닝 정원 해제 완료 (카운터 삭제): diningIdx={}, date={}, time={}", 
                        diningIdx, reservationDate, reservationTime);
            } else {
                log.info("다이닝 정원 해제 완료: diningIdx={}, date={}, time={}, after={}", 
                        diningIdx, reservationDate, reservationTime, newCount);
            }

        } catch (Exception e) {
            log.error("다이닝 정원 해제 중 예외 발생: diningIdx={}, date={}, time={}, error={}", 
                    diningIdx, reservationDate, reservationTime, e.getMessage(), e);
            // 정원 해제 실패는 무시 (TTL로 자동 만료됨)
        }
    }

    /**
     * 현재 예약 인원 조회
     * 
     * @param diningIdx 다이닝 ID
     * @param reservationDate 예약 날짜
     * @param reservationTime 예약 시간
     * @return 현재 예약 인원 수
     */
    public int getCurrentCapacity(Integer diningIdx, LocalDate reservationDate, LocalTime reservationTime) {
        String capacityKey = CAPACITY_PREFIX + diningIdx + ":" + reservationDate + ":" + reservationTime;
        String currentCountStr = redisTemplate.opsForValue().get(capacityKey);
        return currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
    }
}

