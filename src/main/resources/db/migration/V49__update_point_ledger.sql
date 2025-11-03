-- V49__update_point_ledger.sql
ALTER TABLE pointLedger
    CHANGE COLUMN amount point INT NOT NULL,
    CHANGE COLUMN type pointType VARCHAR(30) NOT NULL;

-- 신규 컬럼 추가
ALTER TABLE pointLedger
    ADD COLUMN cash INT DEFAULT 0 AFTER point,
    ADD COLUMN cashType VARCHAR(30) DEFAULT NULL AFTER cash,
    ADD COLUMN orderIdx INT NULL AFTER customerIdx;

-- 외래키 제약조건 추가
ALTER TABLE pointLedger
    ADD CONSTRAINT fk_pointLedger_roomPayment
        FOREIGN KEY (orderIdx)
            REFERENCES roomPayment(orderIdx)
            ON DELETE CASCADE
            ON UPDATE CASCADE;