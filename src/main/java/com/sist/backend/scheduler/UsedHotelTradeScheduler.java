package com.sist.backend.scheduler;

import com.sist.backend.service.UsedTradeService;
import com.sist.backend.service.hotel.UsedHotelTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 중고 호텔 거래 관련 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsedHotelTradeScheduler {

    private final UsedHotelTradeService tradeService;
    private final UsedTradeService usedTradeService;

    /**
     * 만료된 거래 정리 (1분마다 실행)
     * 1분 이상 대기 상태인 거래를 자동 취소 (테스트용)
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    public void cleanupExpiredTrades() {
        try {
            log.debug("만료된 거래 정리 시작");
            tradeService.cleanupExpiredTrades();
        } catch (Exception e) {
            log.error("만료된 거래 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 거래 상태 모니터링 (10분마다 실행)
     * 시스템 상태 확인 및 로깅
     */
    @Scheduled(fixedRate = 600000) // 10분 = 600,000ms
    public void monitorTradeStatus() {
        try {
            log.debug("거래 상태 모니터링 시작");
            // 필요시 추가 모니터링 로직 구현
        } catch (Exception e) {
            log.error("거래 상태 모니터링 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 양도매물 만료 처리 (매일 자정 실행)
     * checkin 날짜가 오늘 날짜와 같거나 이전인 매물을 만료 상태로 변경
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 (00:00:00)
    public void expireUsedItems() {
        try {
            log.info("양도매물 만료 처리 시작");
            int expiredCount = usedTradeService.expireUsedItems();
            log.info("양도매물 만료 처리 완료: {}개의 매물이 만료되었습니다.", expiredCount);
        } catch (Exception e) {
            log.error("양도매물 만료 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
