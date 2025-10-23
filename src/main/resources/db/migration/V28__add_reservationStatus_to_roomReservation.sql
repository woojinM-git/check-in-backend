-- ==========================================
-- V28__add_reservationStatus_to_roomReservation.sql
-- 목적: roomReservation 테이블에 예약 상태 컬럼 추가
-- ==========================================

ALTER TABLE roomReservation
    ADD COLUMN reservationStatus ENUM(
    '예약대기',      -- 결제 전 혹은 임시 예약
    '예약확정',      -- 결제 완료
    '이용중',        -- 체크인 완료, 아직 체크아웃 전
    '이용완료',      -- 체크아웃 완료
    '취소',          -- 예약 취소
    '노쇼'           -- 미이용
) DEFAULT '예약대기' AFTER orderIdx;