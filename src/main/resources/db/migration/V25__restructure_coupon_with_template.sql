-- =============================================
-- CouponTemplate 구조로 쿠폰 테이블 재구성 (템플릿 분리 방식)
-- =============================================

-- 1. 외래키 제약조건 해제
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 기존 테이블들 삭제 (의존성 순서대로)
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS couponPolicy;

-- 3. CouponTemplate 테이블 생성 (Master가 관리하는 쿠폰 템플릿)
CREATE TABLE couponTemplate (
    templateIdx INT PRIMARY KEY AUTO_INCREMENT COMMENT '템플릿 고유번호',
    templateName VARCHAR(100) NOT NULL COMMENT '템플릿명 (예: 여름휴가 쿠폰, 생일축하 쿠폰)',
    discount INT NOT NULL COMMENT '할인 금액',
    validDays INT DEFAULT 30 COMMENT '유효 기간 (일)',
    status TINYINT(1) DEFAULT 1 COMMENT '활성화 여부 (0:비활성, 1:활성)',
    adminIdx INT NOT NULL COMMENT '생성한 Master 번호',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    -- 인덱스 (컬럼명에 맞게 수정)
    INDEX idx_coupon_template_status (status),
    INDEX idx_coupon_template_admin (adminIdx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰 템플릿 (Master 관리)';

-- 4. 새로운 Coupon 테이블 생성
CREATE TABLE coupon (
    couponIdx INT PRIMARY KEY AUTO_INCREMENT COMMENT '쿠폰 고유번호',
    templateIdx INT NOT NULL COMMENT '쿠폰 템플릿 번호',
    id INT NOT NULL COMMENT '발급받은 고객 번호',
    adminIdx INT NOT NULL COMMENT '발급한 관리자 번호',
    createDate DATETIME NOT NULL COMMENT '발급일',
    endDate DATETIME NOT NULL COMMENT '만료일',
    status TINYINT(1) DEFAULT 0 COMMENT '사용상태 (0:미사용, 1:사용완료)',

    -- 외래키 제약조건 (컬럼명에 맞게 수정)
    CONSTRAINT fk_coupon_template FOREIGN KEY (templateIdx) REFERENCES couponTemplate(templateIdx),
    CONSTRAINT fk_coupon_customer FOREIGN KEY (id) REFERENCES customer(customerIdx),
    CONSTRAINT fk_coupon_admin FOREIGN KEY (adminIdx) REFERENCES admin(adminIdx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰 정보';

-- 5. Coupon 테이블 성능 최적화를 위한 인덱스 생성 (컬럼명에 맞게 수정)
CREATE INDEX idx_coupon_customer ON coupon(id);
CREATE INDEX idx_coupon_template ON coupon(templateIdx);
CREATE INDEX idx_coupon_status ON coupon(status);
CREATE INDEX idx_coupon_customer_template ON coupon(id, templateIdx);
CREATE INDEX idx_coupon_create_date ON coupon(createDate);
CREATE INDEX idx_coupon_end_date ON coupon(endDate);

-- 6. 외래키 제약조건 재설정
SET FOREIGN_KEY_CHECKS = 1;