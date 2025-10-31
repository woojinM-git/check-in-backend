-- customer 테이블에 refreshToken 재발급 시간 컬럼 추가
ALTER TABLE `checkin`.`customer` 
ADD COLUMN `refTokenUpdatedAt` DATETIME NULL AFTER `refToken`;

-- admin 테이블에 refreshToken 재발급 시간 컬럼 추가
ALTER TABLE `checkin`.`admin` 
ADD COLUMN `refTokenUpdatedAt` DATETIME NULL AFTER `refToken`;

