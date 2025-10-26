ALTER TABLE `checkin`.`couponTemplate` 
CHANGE COLUMN `status` `status` INT NULL DEFAULT '1' COMMENT '활성화 여부 (0:비활성, 1:활성, 2:삭제)' ;
