-- ==========================================
-- V63__add_serviceStartDate_to_hotelInfo.sql
-- 목적: hotelInfo 테이블에 서비스 시작일(serviceStartDate) 컬럼 추가
-- ==========================================

-- hotelInfo 테이블에 serviceStartDate 컬럼 추가 (DATE 타입, NULL 허용)
ALTER TABLE hotelInfo 
ADD COLUMN serviceStartDate DATE NULL COMMENT '서비스 시작일 (호텔 승인일 기준)' AFTER count;

-- 기존 호텔 데이터의 경우 RegistrationRequest의 approvDate를 기준으로 업데이트
UPDATE hotelInfo h
INNER JOIN registrationRequest rr ON h.adminIdx = rr.adminIdx
SET h.serviceStartDate = DATE(rr.approvDate)
WHERE rr.status = 1 
  AND rr.approvDate IS NOT NULL
  AND h.serviceStartDate IS NULL;

