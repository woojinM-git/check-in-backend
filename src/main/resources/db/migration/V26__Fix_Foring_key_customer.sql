ALTER TABLE `checkin`.`coupon`
DROP FOREIGN KEY `fk_coupon_customer`;
ALTER TABLE `checkin`.`coupon`
    CHANGE COLUMN `id` `customerIdx` INT NOT NULL COMMENT '발급받은 고객 번호' ;
ALTER TABLE `checkin`.`coupon`
    ADD CONSTRAINT `fk_coupon_customer`
        FOREIGN KEY (`customerIdx`)
            REFERENCES `checkin`.`customer` (`customerIdx`);
