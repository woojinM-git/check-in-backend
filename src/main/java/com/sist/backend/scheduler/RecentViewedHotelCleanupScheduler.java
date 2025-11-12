package com.sist.backend.scheduler;

import com.sist.backend.service.mypage.RecentViewedHotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 최근 본 호텔 자동 정리 스케줄러
 * createdAt 기준 1개월이 지난 기록을 매일 자정에 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecentViewedHotelCleanupScheduler {

    private static final long RETENTION_MONTHS = 1L;

    private final RecentViewedHotelService recentViewedHotelService;

    /**
     * 매일 자정(00:00)에 1개월 이상 지난 최근 본 호텔 기록 삭제
     * cron 표현식: 초(0) 분(0) 시(0) 일(*) 월(*) 요일(?)
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void cleanUpExpiredRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(RETENTION_MONTHS);

        try {
            long deletedCount = recentViewedHotelService.deleteRecentViewedHotelsOlderThan(cutoff);
            if (deletedCount > 0) {
                log.info("최근 본 호텔 자동 정리 완료 - 기준일: {}, 삭제건수: {}", cutoff, deletedCount);
            } else {
                log.debug("최근 본 호텔 자동 정리 - 기준일: {} 삭제 대상 없음", cutoff);
            }
        } catch (Exception e) {
            log.error("최근 본 호텔 자동 정리 실패 - 기준일: {}", cutoff, e);
        }
    }
}


