ALTER TABLE `checkin`.`hotelInfo` 
ADD COLUMN `stopReason` TEXT NULL DEFAULT NULL AFTER `status`;
