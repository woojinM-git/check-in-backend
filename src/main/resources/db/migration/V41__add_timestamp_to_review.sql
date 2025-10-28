-- Review 테이블에 타임스탬프 컬럼 추가
ALTER TABLE review ADD COLUMN createdAt DATETIME NULL;
ALTER TABLE review ADD COLUMN updatedAt DATETIME NULL;

-- 기존 데이터의 타임스탬프를 현재 시간으로 설정
UPDATE review SET createdAt = NOW() WHERE createdAt IS NULL;
UPDATE review SET updatedAt = NOW() WHERE updatedAt IS NULL;

-- NOT NULL 제약조건 추가
ALTER TABLE review MODIFY COLUMN createdAt DATETIME NOT NULL;
ALTER TABLE review MODIFY COLUMN updatedAt DATETIME NOT NULL;

