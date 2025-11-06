-- V61: roomImage 테이블 제약조건 수정
-- 삭제된 이미지(status=0)와 활성 이미지(status=1)를 구분하여 UNIQUE 제약조건 적용

-- 기존 UNIQUE 제약조건 삭제
SET @unique_constraint_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'roomImage' 
    AND CONSTRAINT_TYPE = 'UNIQUE'
    AND CONSTRAINT_NAME = 'uk_roomImage_room_order'
    LIMIT 1
);

SET @drop_unique_sql = IF(
    @unique_constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE ', DATABASE(), '.roomImage DROP INDEX ', @unique_constraint_name),
    'SELECT 1'
);

PREPARE drop_unique_stmt FROM @drop_unique_sql;
EXECUTE drop_unique_stmt;
DEALLOCATE PREPARE drop_unique_stmt;

-- 새로운 UNIQUE 제약조건 추가 (status 포함)
-- 활성 이미지(status=1)와 삭제된 이미지(status=0)를 구분하여 중복 허용
ALTER TABLE roomImage 
ADD CONSTRAINT uk_roomImage_room_order_status 
UNIQUE (roomIdx, contentId, imageOrder, status);

-- 기존 CHECK 제약조건 삭제
SET @check_constraint_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'roomImage' 
    AND CONSTRAINT_TYPE = 'CHECK'
    AND CONSTRAINT_NAME = 'chk_roomImage_order'
    LIMIT 1
);

SET @drop_check_sql = IF(
    @check_constraint_name IS NOT NULL,
    CONCAT('ALTER TABLE ', DATABASE(), '.roomImage DROP CHECK ', @check_constraint_name),
    'SELECT 1'
);

PREPARE drop_check_stmt FROM @drop_check_sql;
EXECUTE drop_check_stmt;
DEALLOCATE PREPARE drop_check_stmt;

-- 새로운 CHECK 제약조건 추가 (삭제된 이미지는 imageOrder >= 1000 허용)
-- 활성 이미지는 imageOrder = 1~10, 삭제된 이미지는 imageOrder >= 1000 (roomImageIdx 사용)
ALTER TABLE roomImage 
ADD CONSTRAINT chk_roomImage_order 
CHECK ((status = 0 AND imageOrder >= 1000) OR (status = 1 AND imageOrder >= 1 AND imageOrder <= 10));


