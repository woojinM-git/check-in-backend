-- =====================================================
-- V32: diningPayment 테이블 구조 개선
-- =====================================================

-- diningPayment는 이미 대부분의 필드가 있음
-- 누락된 필드만 추가하고, 타임스탬프 보완

-- 1. 타임스탬프 컬럼 확인 및 보완
-- createdAt, approvedAt, updatedAt는 이미 존재

-- 2. 인덱스 추가 (성능 최적화)
CREATE INDEX idx_dining_payment_customer ON diningPayment(customerIdx);
CREATE INDEX idx_dining_payment_status ON diningPayment(status);
CREATE INDEX idx_dining_payment_key ON diningPayment(paymentKey);
CREATE INDEX idx_dining_payment_dining ON diningPayment(diningIdx);

-- 3. 컬럼 comment 업데이트 (각각 분리)
ALTER TABLE diningPayment MODIFY COLUMN status INT COMMENT '0:대기, 1:완료, 2:취소, 3:환불';
ALTER TABLE diningPayment MODIFY COLUMN paymentKey VARCHAR(50) COMMENT '토스페이먼츠 결제 키';
ALTER TABLE diningPayment MODIFY COLUMN method VARCHAR(255) COMMENT '결제 수단 (카드, 계좌이체 등)';
ALTER TABLE diningPayment MODIFY COLUMN receiptUrl VARCHAR(500) COMMENT '영수증 URL';
ALTER TABLE diningPayment MODIFY COLUMN price INT COMMENT '실제 결제 금액';
ALTER TABLE diningPayment MODIFY COLUMN pointUsed INT COMMENT '사용한 포인트';
ALTER TABLE diningPayment MODIFY COLUMN couponIdx INT COMMENT '사용한 쿠폰 ID';

