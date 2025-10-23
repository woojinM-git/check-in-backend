-- V27: Update room table data and add status column
-- Description: Update capacity values (2→4, 4→8), roomCount (5→1), and add status column

-- 1. Add status column to room table
ALTER TABLE room ADD COLUMN status INT NOT NULL DEFAULT 1 COMMENT '0: 사용불가, 1: 사용가능';

-- 2. Update capacity values: 2 → 4, 4 → 8
UPDATE room SET capacity = 8 WHERE capacity = 4;
UPDATE room SET capacity = 4 WHERE capacity = 2;


-- 3. Update roomCount values: 5 → 1
UPDATE room SET roomCount = 1 WHERE roomCount = 5;

-- 4. Set all rooms as available by default (status = 1)
UPDATE room SET status = 1 WHERE status IS NULL;
