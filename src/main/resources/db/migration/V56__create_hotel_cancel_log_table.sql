-- V56__create_hotel_cancel_log_table.sql
-- 호텔 OTA 플랫폼 - 호텔 예약 취소 로그 테이블 생성

CREATE TABLE hotelCancelLog (
                                cancelId BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '취소 로그 고유 ID',
                                reservIdx INT NOT NULL COMMENT 'roomReservation 테이블의 PK (예약 ID)',
                                cancelReason VARCHAR(1000) NULL COMMENT '취소 사유',
                                refundTotalAmount INT NULL COMMENT '환불 총 금액 (수수료 제외, 쿠폰 환불 제외)',
                                refundCash INT NULL COMMENT '환불된 캐시 금액',
                                refundPoint INT NULL COMMENT '환불된 포인트 금액',
                                refundStatus INT NOT NULL DEFAULT 0 COMMENT '0: 환불 진행중 / 1: 환불 완료 / 2: 환불 실패 / 3: 관리자 거절',
                                cancelAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '취소 시각',
                                canceledBy VARCHAR(20) NOT NULL COMMENT '취소자 (USER, ADMIN, SYSTEM)',
                                CONSTRAINT fk_hotelCancelLog_reservation FOREIGN KEY (reservIdx)
                                    REFERENCES roomReservation(reservIdx)
                                    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='호텔 예약 취소 로그 테이블';
