-- roomImage와 hotelImage 테이블에 소프트 삭제 기능 추가
-- status: 0 = 삭제됨, 1 = 활성
-- deletedAt: 삭제 시간 (NULL이면 활성 상태)

-- 1. roomImage 테이블에 status, deletedAt 컬럼 추가
ALTER TABLE roomImage 
ADD COLUMN status INT NOT NULL DEFAULT 1 COMMENT '0: 삭제됨, 1: 활성',
ADD COLUMN deletedAt TIMESTAMP NULL COMMENT '삭제 시간';

-- 2. hotelImage 테이블에 status, deletedAt 컬럼 추가
ALTER TABLE hotelImage 
ADD COLUMN status INT NOT NULL DEFAULT 1 COMMENT '0: 삭제됨, 1: 활성',
ADD COLUMN deletedAt TIMESTAMP NULL COMMENT '삭제 시간';

-- 3. hotelImage 테이블에 복합 인덱스 추가 (조회 성능 최적화)
-- (contentId, status, id) 복합 인덱스로 WHERE와 ORDER BY 모두 커버
CREATE INDEX idx_hotelImage_content_status_id ON hotelImage(contentId, status, id);

