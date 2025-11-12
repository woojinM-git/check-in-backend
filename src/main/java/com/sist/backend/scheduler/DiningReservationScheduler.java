package com.sist.backend.scheduler;

import com.sist.backend.service.DiningReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다이닝 예약 관련 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiningReservationScheduler {

    private final DiningReservationService diningReservationService;

    /**
     * 서버 시작 시 한 번 실행 (애플리케이션이 완전히 준비된 후 실행)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        log.info("서버 시작 시 예약 날짜가 지난 다이닝 예약 이용완료 처리 실행");
        completeExpiredReservations();
    }

    /**
     * 예약 날짜가 지난 다이닝 예약을 이용완료로 자동 변경 (매일 09시 실행)
     * 예약 날짜가 어제 이하인 확정(status=1) 예약을 이용완료(status=4)로 변경
     */
    @Scheduled(cron = "0 0 9 * * ?") // 매일 09시 정각 실행
    public void completeExpiredReservations() {
        try {
            log.info("예약 날짜가 지난 다이닝 예약 이용완료 처리 시작");
            int completedCount = diningReservationService.completeExpiredReservations();
            log.info("예약 날짜가 지난 다이닝 예약 이용완료 처리 완료: {}개의 예약이 이용완료로 변경되었습니다.", completedCount);
        } catch (Exception e) {
            log.error("예약 날짜가 지난 다이닝 예약 이용완료 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}

