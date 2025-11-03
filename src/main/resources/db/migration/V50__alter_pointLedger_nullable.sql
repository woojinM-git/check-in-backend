-- V50__alter_pointLedger_nullable.sql
ALTER TABLE pointLedger
    MODIFY COLUMN point INT NULL,
    MODIFY COLUMN pointType VARCHAR(30) NULL;
