package com.sist.backend.scheduler;

import com.sist.backend.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * 정산 배치 스케줄러
 * 매월 1일 자정에 직전 달 정산 데이터를 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementService settlementService;

    /**
     * 매월 1일 00:00:00에 직전 달 정산 데이터 생성
     * 비동기 처리로 응답 지연 없음
     */
    @Async
    @Scheduled(cron = "0 0 0 1 * ?") // 매월 1일 자정
    public void createMonthlySettlement() {
        try {
            log.info("=== 정산 배치 작업 시작 ===");
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            
            log.info("정산 대상 월: {}년 {}월", previousMonth.getYear(), previousMonth.getMonthValue());
            settlementService.createSettlementForMonth(
                previousMonth.getYear(), 
                previousMonth.getMonthValue()
            );
            
            log.info("=== 정산 배치 작업 완료 ===");
        } catch (Exception e) {
            log.error("정산 배치 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}

