ALTER TABLE `checkin`.`customer` 
CHANGE COLUMN `id` `id` VARCHAR(255) NOT NULL ,
CHANGE COLUMN `provider` `provider` INT NULL DEFAULT NULL COMMENT '0: 네이버, 1: 카카오, 2: 구글' ;
