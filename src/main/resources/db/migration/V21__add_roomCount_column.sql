-- V21: Add roomCount column if tmpIdx no longer exists

--  roomCount 컬럼이 없으면 추가
ALTER TABLE room
    ADD COLUMN roomCount INT DEFAULT 0 COMMENT '객실 수';

-- 모든 방 기본 객실 수를 5로 초기화
UPDATE room
SET roomCount = 5
WHERE roomCount IS NULL OR roomCount = 0;
