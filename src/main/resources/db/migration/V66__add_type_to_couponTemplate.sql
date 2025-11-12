-- couponTemplate 테이블에 type 컬럼 추가
-- type: 0 = 단체가 아닌 지정발급형식, 1 = 단체 발급형식
ALTER TABLE couponTemplate ADD COLUMN type INT DEFAULT 0 NOT NULL COMMENT '쿠폰 타입 (0: 지정발급형식, 1: 단체 발급형식)';

