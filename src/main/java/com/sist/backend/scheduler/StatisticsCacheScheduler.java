package com.sist.backend.scheduler;

import com.sist.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 통계 데이터 캐시 갱신 스케줄러
 * 매일 새벽 6시에 지역별 통계 캐시를 갱신하여 하루 동안 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsCacheScheduler {

    private final StatisticsService statisticsService;

    /**
     * 서버 시작 시 지역별 통계 캐시 초기화
     * ApplicationReadyEvent: 모든 빈이 준비된 후 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCacheOnStartup() {
        try {
            log.info("=== 서버 시작 시 지역별 통계 캐시 초기화 시작 ===");
            statisticsService.refreshRegionStatisticsCache();
            log.info("=== 서버 시작 시 지역별 통계 캐시 초기화 완료 ===");
        } catch (Exception e) {
            log.error("서버 시작 시 지역별 통계 캐시 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 새벽 6시에 지역별 통계 캐시 갱신
     * cron 표현식: "0 0 6 * * ?"
     * - 초(0) 분(0) 시(6) 일(*) 월(*) 요일(?)
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void refreshRegionStatisticsCache() {
        try {
            log.info("=== 지역별 통계 캐시 갱신 시작 (매일 새벽 6시) ===");
            statisticsService.refreshRegionStatisticsCache();
            log.info("=== 지역별 통계 캐시 갱신 완료 ===");
        } catch (Exception e) {
            log.error("지역별 통계 캐시 갱신 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}

