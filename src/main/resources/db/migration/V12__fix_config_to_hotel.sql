ALTER TABLE `checkin`.`hotelInfo` 
ADD COLUMN `adminIdx` INT NULL AFTER `status`;

UPDATE `checkin`.`hotelInfo` SET `adminIdx` = '1' WHERE (`contentId` = '1003654');
UPDATE `checkin`.`hotelInfo` SET `adminIdx` = '1' WHERE (`contentId` = '1034361');
UPDATE `checkin`.`hotelInfo` SET `adminIdx` = '1' WHERE (`contentId` = '1037300');