-- =====================================================
-- V30: 다이닝 시간대별 예약 시스템 구조 개선
-- =====================================================

-- 1. dining 테이블 개선 - 시간대 관리 컬럼 추가
ALTER TABLE dining ADD COLUMN name VARCHAR(100) COMMENT '다이닝 이름 (예: 조식 뷔페, 석식 코스)';
ALTER TABLE dining ADD COLUMN description TEXT COMMENT '다이닝 설명';
ALTER TABLE dining ADD COLUMN imageUrl VARCHAR(500) COMMENT '다이닝 이미지 URL';
ALTER TABLE dining ADD COLUMN totalSeats INT COMMENT '총 좌석 수';
ALTER TABLE dining ADD COLUMN basePrice INT COMMENT '1인당 기본 가격';
ALTER TABLE dining ADD COLUMN openTime TIME COMMENT '오픈 시간 (예: 07:00)';
ALTER TABLE dining ADD COLUMN closeTime TIME COMMENT '마감 시간 (예: 10:00)';
ALTER TABLE dining ADD COLUMN slotDuration INT DEFAULT 30 COMMENT '예약 시간 단위 (분)';
ALTER TABLE dining ADD COLUMN maxGuestsPerSlot INT COMMENT '시간대별 최대 인원';
ALTER TABLE dining ADD COLUMN status INT DEFAULT 1 COMMENT '0:비활성, 1:활성';
ALTER TABLE dining ADD COLUMN createdAt DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dining ADD COLUMN updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2. 기존 컬럼 comment 업데이트 (DEPRECATED 표시)
ALTER TABLE dining MODIFY COLUMN time VARCHAR(50) COMMENT '[DEPRECATED] 시간 정보 (openTime/closeTime 사용)';
ALTER TABLE dining MODIFY COLUMN price INT NOT NULL COMMENT '[DEPRECATED] 가격 정보 (basePrice 사용)';
ALTER TABLE dining MODIFY COLUMN date DATE NOT NULL COMMENT '[DEPRECATED] 날짜 정보 (예약 테이블에서 관리)';
ALTER TABLE dining MODIFY COLUMN totalCount INT COMMENT '[DEPRECATED] 총 수량 (totalSeats 사용)';
ALTER TABLE dining MODIFY COLUMN bookedCount INT COMMENT '[DEPRECATED] 예약된 수량 (쿼리 집계로 대체)';

