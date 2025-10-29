-- V42__remove_paymentStatus_and_reservationStatus.sql
-- roomPayment.paymentStatus, roomReservation.reservationStatus 컬럼 삭제

ALTER TABLE roomPayment
DROP COLUMN paymentStatus;

ALTER TABLE roomReservation
DROP COLUMN reservationStatus;
