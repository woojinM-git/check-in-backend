-- ==========================================
-- V29__add_paymentStatus_to_roomPayment.sql
-- 목적: roomPayment 테이블에 결제 상태 컬럼 추가
-- ==========================================

ALTER TABLE roomPayment
    ADD COLUMN paymentStatus ENUM(
    '결제대기',      -- 결제 진행 전
    '결제완료',      -- 결제 완료
    '결제취소',      -- 결제 취소됨
    '환불대기',      -- 환불 요청 접수됨
    '환불완료'       -- 환불 처리 완료
) DEFAULT '결제대기' AFTER price;