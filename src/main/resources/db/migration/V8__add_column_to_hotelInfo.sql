ALTER TABLE `checkin`.`hotelInfo` 
ADD COLUMN `status` TINYINT(1) NULL DEFAULT 1 AFTER `imageUrl`;