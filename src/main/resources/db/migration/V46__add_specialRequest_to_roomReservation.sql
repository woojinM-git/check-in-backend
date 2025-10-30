-- V46__add_specialRequest_to_roomReservation.sql
-- 고객 요청사항(specialRequest) 컬럼 추가

ALTER TABLE roomReservation
    ADD COLUMN specialRequest VARCHAR(1000) NULL
AFTER totalPrice;
