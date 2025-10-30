-- V45__add_orderNum_to_roomReservation.sql
-- roomReservation 테이블에 주문번호(orderNum) 컬럼 추가

ALTER TABLE roomReservation
    ADD COLUMN orderNum VARCHAR(100) NULL UNIQUE
    AFTER orderIdx;

-- 주석:
-- orderNum 예시: hotel_2434843-8337_52ve13r4b
-- 예약 시 백엔드에서 UUID 기반으로 생성되어 QR, 결제, 고객 문의 등에서 사용됨.
