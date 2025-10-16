ALTER TABLE `checkin`.`registrationRequest` 
CHANGE COLUMN `registrationIdx` `registrationIdx` INT NOT NULL AUTO_INCREMENT ,
CHANGE COLUMN `status` `status` TINYINT(1) NULL DEFAULT 0 ,
ADD PRIMARY KEY (`registrationIdx`);