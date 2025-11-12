package com.sist.backend.service;

import com.sist.backend.repository.DiningReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다이닝 예약 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiningReservationService {

    private final DiningReservationRepository diningReservationRepository;

    /**
     * 예약 날짜가 지난 예약확정(status=1) 예약을 이용완료(status=4)로 변경
     * 예약 날짜가 어제 이하인 예약을 처리
     * 
     * 처리 조건:
     * - 예약 날짜 < 어제 (예약 날짜 < 현재 날짜 - 1일) → 항상 처리
     * - 예약 날짜 = 어제 (예약 날짜 = 현재 날짜 - 1일) → 오늘 처리
     * 
     * 스케줄러가 매일 실행되므로, 예약 날짜가 어제 이하인 모든 예약을 처리
     * 
     * @return 변경된 예약의 개수
     */
    @Transactional
    public int completeExpiredReservations() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate yesterday = today.minusDays(1);
        
        // 예약 날짜가 어제 이하인 모든 예약 처리
        log.info("다이닝 예약 이용완료 처리 시작: targetDate={}", yesterday);
        int completedCount = diningReservationRepository.updateExpiredReservationsToCompleted(yesterday);
        log.info("다이닝 예약 이용완료 처리 완료: {}개의 예약이 이용완료로 변경되었습니다.", completedCount);
        
        return completedCount;
    }
}

