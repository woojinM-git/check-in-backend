-- 1. 기존 데이터의 status 값을 0으로 업데이트
UPDATE `checkin`.`hotelInfo` SET `status` = 0 WHERE `status` = 1;

-- 2. 컬럼의 DEFAULT 값을 0으로 변경
ALTER TABLE `checkin`.`hotelInfo` 
CHANGE COLUMN `status` `status` INT NULL DEFAULT 0;