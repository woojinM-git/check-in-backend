-- registrationRequest 테이블 수정
-- contentid 컬럼 제거하고 draftIdx 컬럼 추가

-- 1. contentid 컬럼 제거
ALTER TABLE `registrationRequest` DROP COLUMN `contentid`;

-- 2. draftIdx 컬럼 추가
ALTER TABLE `registrationRequest` ADD COLUMN `draftIdx` INT NULL AFTER `adminIdx`;

-- 3. HotelDraft 테이블과의 FK 제약조건 추가
ALTER TABLE `registrationRequest` 
ADD CONSTRAINT `fk_registrationRequest_hotelDraft` 
FOREIGN KEY (`draftIdx`) REFERENCES `hotelDraft` (`draftIdx`);
