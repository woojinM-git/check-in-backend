-- V51__add_cash_used_to_roomPayment.sql

ALTER TABLE roomPayment
    ADD COLUMN cashUsed INT NULL AFTER pointsUsed;