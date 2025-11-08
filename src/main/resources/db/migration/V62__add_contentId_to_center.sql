-- ==========================================
-- V62__add_contentId_to_center.sql
-- 목적: center 테이블에 호텔 contentId 참조 컬럼 추가
-- ==========================================

-- center 테이블에 contentId 컬럼 추가 (NULL 허용)
ALTER TABLE center 
ADD COLUMN contentId VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '호텔 ID (호텔 관련 문의 시 참조)' AFTER customerIdx;

-- 외래키 제약조건 추가 (hotelInfo 테이블의 contentId 참조)
ALTER TABLE center 
ADD CONSTRAINT FK_center_hotelInfo 
    FOREIGN KEY (contentId) REFERENCES hotelInfo(contentId) 
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 인덱스 추가 (호텔별 문의 조회 성능 향상)
ALTER TABLE center 
ADD INDEX idx_contentId (contentId);

