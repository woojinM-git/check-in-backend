-- V60: hotelImage 인덱스 수정
-- 기존 (contentId, id) 인덱스를 (contentId, status, id)로 변경
-- V59에서 인덱스가 이미 존재하여 생성 실패한 경우를 대비

-- 기존 인덱스 삭제 (존재하지 않으면 에러 무시)
SET @index_exists = (
    SELECT COUNT(*) 
    FROM information_schema.statistics 
    WHERE table_schema = DATABASE() 
    AND table_name = 'hotelImage' 
    AND index_name = 'idx_hotelImage_content_status_id'
);

-- 인덱스가 존재하면 삭제
SET @drop_sql = IF(
    @index_exists > 0,
    CONCAT('ALTER TABLE ', DATABASE(), '.hotelImage DROP INDEX idx_hotelImage_content_status_id'),
    'SELECT 1'
);

PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

-- 올바른 구성의 인덱스 생성
CREATE INDEX idx_hotelImage_content_status_id ON hotelImage(contentId, status, id);

