-- =====================================================
-- V31: diningReservation 테이블 구조 개선
-- =====================================================

-- 1. 새로운 컬럼 추가
ALTER TABLE diningResrevation ADD COLUMN reservationDate DATE COMMENT '예약 날짜';
ALTER TABLE diningResrevation ADD COLUMN reservationTime TIME COMMENT '예약 시간 (예: 18:00)';
ALTER TABLE diningResrevation ADD COLUMN qrUrl VARCHAR(500) COMMENT 'QR코드 URL (입장 확인용)';
ALTER TABLE diningResrevation ADD COLUMN specialRequest TEXT COMMENT '특별 요청사항';

-- 2. status 컬럼을 INT로 변경 (VARCHAR -> INT)
-- 데이터가 없으므로 간단하게 처리
ALTER TABLE diningResrevation ADD COLUMN status_new INT DEFAULT 0 COMMENT '0:대기, 1:확정, 2:취소, 3:노쇼, 4:완료';
ALTER TABLE diningResrevation DROP COLUMN status;
ALTER TABLE diningResrevation CHANGE COLUMN status_new status INT DEFAULT 0 COMMENT '0:대기, 1:확정, 2:취소, 3:노쇼, 4:완료';

-- 3. checkIn 컬럼 DEPRECATED 표시
ALTER TABLE diningResrevation MODIFY COLUMN checkIn VARCHAR(255) COMMENT '[DEPRECATED] 예약일시 (reservationDate, reservationTime 사용)';

-- 4. 인덱스 추가 (성능 최적화)
CREATE INDEX idx_dining_reservation_date_time ON diningResrevation(diningIdx, reservationDate, reservationTime);
CREATE INDEX idx_dining_reservation_customer ON diningResrevation(customerIdx);
CREATE INDEX idx_dining_reservation_status ON diningResrevation(status);

