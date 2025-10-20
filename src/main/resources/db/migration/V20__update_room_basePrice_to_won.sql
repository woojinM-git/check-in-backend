-- V21__update_room_basePrice_to_won.sql

-- 1. 기존 값 × 10000으로 업데이트
UPDATE room
SET basePrice = basePrice * 10000;

-- 2. (선택) 컬럼 타입 변경: 소수점 없애고 정수형으로 바꿀 경우
ALTER TABLE room
    MODIFY COLUMN basePrice INT;