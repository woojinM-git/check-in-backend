-- usedItem 테이블에 comment 컬럼 추가
ALTER TABLE usedItem ADD COLUMN comment VARCHAR(255) NULL DEFAULT NULL;