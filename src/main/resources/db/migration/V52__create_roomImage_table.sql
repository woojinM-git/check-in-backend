-- 객실 이미지 테이블 생성
-- 객실마다 최대 10개의 이미지를 저장할 수 있음
-- contentId는 room 테이블의 contentId와 정확히 동일한 타입으로 생성해야 함
CREATE TABLE roomImage (
    roomImageIdx INT AUTO_INCREMENT PRIMARY KEY COMMENT '객실 이미지 고유 인덱스',
    roomIdx INT NOT NULL COMMENT '객실 인덱스',
    contentId VARCHAR(50) NOT NULL COMMENT '호텔 콘텐츠 ID',
    imageUrl VARCHAR(500) NOT NULL COMMENT 'S3 이미지 URL',
    imageOrder INT NOT NULL DEFAULT 1 COMMENT '이미지 순서 (1-10)',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    
    -- 인덱스 추가 (검색 성능 향상)
    INDEX idx_roomImage_room (roomIdx, contentId),
    INDEX idx_roomImage_contentId (contentId),
    
    -- 제약조건: 같은 객실에서 이미지 순서는 1-10 사이여야 함
    CONSTRAINT chk_roomImage_order CHECK (imageOrder >= 1 AND imageOrder <= 10),
    
    -- 제약조건: 같은 객실에서 같은 순서는 중복 불가
    CONSTRAINT uk_roomImage_room_order UNIQUE (roomIdx, contentId, imageOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='객실 이미지 테이블';

-- contentId 컬럼을 room 테이블의 contentId와 정확히 동일한 타입으로 변경
-- room.contentId가 hotelInfo.contentId를 참조하는 외래키이므로 타입이 정확히 일치해야 함
SET @room_contentId_type = (
    SELECT CONCAT(
        'VARCHAR(', CHARACTER_MAXIMUM_LENGTH, ')',
        IF(CHARACTER_SET_NAME IS NOT NULL, 
           CONCAT(' CHARACTER SET ', CHARACTER_SET_NAME), ''),
        IF(COLLATION_NAME IS NOT NULL, 
           CONCAT(' COLLATE ', COLLATION_NAME), ''),
        IF(IS_NULLABLE = 'NO', ' NOT NULL', '')
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'room'
      AND COLUMN_NAME = 'contentId'
);

SET @modify_sql = CONCAT('ALTER TABLE roomImage MODIFY COLUMN contentId ', @room_contentId_type);
PREPARE stmt FROM @modify_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- room 테이블에 (roomIdx, contentId) 복합 인덱스 생성
-- 외래키 제약조건을 추가하려면 참조되는 테이블에 해당 복합 인덱스가 필요함
-- room 테이블은 roomIdx만 PRIMARY KEY이므로, (roomIdx, contentId) 복합 인덱스가 필요
-- 인덱스가 이미 존재하면 오류가 발생하므로, 없을 때만 생성
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'room'
      AND INDEX_NAME = 'idx_room_roomIdx_contentId'
);

SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE room ADD INDEX idx_room_roomIdx_contentId (roomIdx, contentId)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 외래키 제약조건 추가 (테이블 생성 및 컬럼 수정, 인덱스 생성 후 별도로 추가)
ALTER TABLE roomImage
    ADD CONSTRAINT fk_roomImage_room 
        FOREIGN KEY (roomIdx, contentId) 
        REFERENCES room(roomIdx, contentId) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE;

