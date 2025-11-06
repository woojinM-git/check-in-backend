-- V58__create_dining_cancel_log_table.sql
-- 다이닝 예약 취소 로그 테이블 생성
-- 호텔 예약 취소 로그(hotelCancelLog)와 동일한 구조로 다이닝 전용 취소 로그를 관리

CREATE TABLE diningCancelLog (
    cancelId BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '취소 로그 고유 ID',
    diningResrIdx INT NOT NULL COMMENT 'diningReservation 테이블의 예약 PK',
    cancelReason VARCHAR(1000) NULL COMMENT '취소 사유',
    refundTotalAmount INT NULL COMMENT '환불 총 금액 (수수료 제외, 쿠폰 환불 제외)',
    refundCash INT NULL COMMENT '환불된 캐시 금액',
    refundPoint INT NULL COMMENT '환불된 포인트 금액',
    refundStatus INT NOT NULL DEFAULT 0 COMMENT '0: 환불 진행중 / 1: 환불 완료 / 2: 환불 실패 / 3: 관리자 거절',
    cancelAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '취소 시각',
    canceledBy VARCHAR(20) NOT NULL COMMENT '취소자 (USER, ADMIN, SYSTEM)',
    INDEX idx_diningResrIdx (diningResrIdx),
    INDEX idx_refundStatus (refundStatus),
    INDEX idx_cancelAt (cancelAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='다이닝 예약 취소 로그 테이블';

