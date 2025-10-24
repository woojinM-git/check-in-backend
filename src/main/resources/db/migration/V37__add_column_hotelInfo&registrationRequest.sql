ALTER TABLE `checkin`.`hotelInfo`
    ADD COLUMN `count` INT NULL DEFAULT NULL AFTER `adminIdx`;

ALTER TABLE `checkin`.`registrationRequest`
    ADD COLUMN `refusalMsg` TEXT NULL DEFAULT NULL AFTER `approvDate`;