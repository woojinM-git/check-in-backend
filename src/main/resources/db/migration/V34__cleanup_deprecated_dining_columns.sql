-- =====================================================
-- V34: 다이닝 테이블 deprecated 컬럼 정리
-- =====================================================

-- 주의: 이 마이그레이션은 되돌릴 수 없습니다!
-- V30~V33에서 새로운 컬럼으로 완전히 대체되었으므로 안전하게 제거 가능

-- 1. dining 테이블 deprecated 컬럼 제거
-- time → openTime, closeTime으로 대체됨
ALTER TABLE dining DROP COLUMN time;

-- price → basePrice로 대체됨  
ALTER TABLE dining DROP COLUMN price;

-- date → 예약 테이블에서 관리하므로 불필요
ALTER TABLE dining DROP COLUMN date;

-- totalCount → totalSeats로 대체됨
ALTER TABLE dining DROP COLUMN totalCount;

-- bookedCount → 쿼리 집계로 대체됨 (동시성 문제 해결)
ALTER TABLE dining DROP COLUMN bookedCount;

-- 2. diningResrevation 테이블 deprecated 컬럼 제거
-- checkIn → reservationDate, reservationTime으로 대체됨
ALTER TABLE diningResrevation DROP COLUMN checkIn;

-- 3. 정리 완료 로그
-- 이제 다이닝 테이블들이 깔끔하게 정리되었습니다!
-- 새로운 시간대별 예약 시스템만 사용 가능합니다.
