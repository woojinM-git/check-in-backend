package com.sist.backend.scheduler;

import com.sist.backend.service.RoomReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 관련 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomReservationScheduler {

    private final RoomReservationService roomReservationService;

    /**
     * 서버 시작 시 한 번 실행 (애플리케이션이 완전히 준비된 후 실행)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        log.info("서버 시작 시 체크아웃 시간이 지난 예약 이용완료 처리 실행");
        completeExpiredReservations();
    }

    /**
     * 체크아웃 시간이 지난 예약을 이용완료로 자동 변경 (매일 15시 실행)
     * 체크아웃 날짜의 15시부터 24시간이 지난 예약을 이용완료(4)로 변경
     * 체크아웃은 15시 전에 이루어지므로, 체크아웃 날짜 + 1일의 15시에 실행하면 충분
     */
    @Scheduled(cron = "0 0 9 * * ?") // 매일 09시 정각 실행
    public void completeExpiredReservations() {
        try {
            log.info("체크아웃 시간이 지난 예약 이용완료 처리 시작");
            int completedCount = roomReservationService.completeExpiredReservations();
            log.info("체크아웃 시간이 지난 예약 이용완료 처리 완료: {}개의 예약이 이용완료로 변경되었습니다.", completedCount);
        } catch (Exception e) {
            log.error("체크아웃 시간이 지난 예약 이용완료 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}

