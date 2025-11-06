-- ==========================================
-- V57__create_hotelSettlement_table.sql
-- 목적: 호텔 정산 테이블 생성
-- ==========================================

CREATE TABLE IF NOT EXISTS hotelSettlement (
    settlementIdx INT AUTO_INCREMENT PRIMARY KEY COMMENT '정산 장부 ID',
    contentId VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '호텔 ID',
    settlementMonth VARCHAR(7) NOT NULL COMMENT '정산 월 (형식: 2024-01)',
    totalRevenue BIGINT NOT NULL COMMENT '월 수익금액 (원)',
    commissionAmount BIGINT NOT NULL COMMENT '사이트 수수료 금액 (원)',
    withholdingTaxAmount BIGINT NOT NULL COMMENT '원천징수 금액 (원)',
    finalAmount BIGINT NOT NULL COMMENT '실 지급액 (수수료 및 원천징수 제외)',
    CONSTRAINT fk_hotelSettlement_hotelInfo FOREIGN KEY (contentId) REFERENCES hotelInfo(contentId),
    UNIQUE KEY uk_contentId_settlementMonth (contentId, settlementMonth),
    INDEX idx_settlementMonth (settlementMonth)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

