package com.sist.backend.scheduler;

import com.sist.backend.service.RoomReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 예약 관련 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomReservationScheduler {

    private final RoomReservationService roomReservationService;

    /**
     * 체크아웃 시간이 지난 예약을 이용완료로 자동 변경 (매일 15시 실행)
     * 체크아웃 날짜의 15시부터 24시간이 지난 예약을 이용완료(4)로 변경
     * 체크아웃은 15시 전에 이루어지므로, 체크아웃 날짜 + 1일의 15시에 실행하면 충분
     */
    @Scheduled(cron = "0 0 9 * * ?") // 매일 15시 정각 실행
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

