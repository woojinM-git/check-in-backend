-- Center 테이블을 고객센터 게시판으로 변형
-- 트랜잭션으로 감싸서 한번에 적용되거나 한번에 롤백되도록 처리

START TRANSACTION;

-- 1. 기존 데이터 백업 (필요시)
-- CREATE TABLE center_backup AS SELECT * FROM center;

-- 2. 기존 외래키 제약조건 삭제
ALTER TABLE center DROP FOREIGN KEY FK_admin_TO_center_1;

-- 3. 새로운 컬럼 추가
ALTER TABLE center 
ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT '' COMMENT '제목' AFTER centerIdx,
ADD COLUMN priority INT DEFAULT 0 COMMENT '우선순위 (0: 일반, 1: 높음, 2: 긴급)' AFTER type,
ADD COLUMN subCategory VARCHAR(50) NULL COMMENT '세부 카테고리 (예약/취소, 회원정보, 기술지원, 결제, 호텔정보 등)' AFTER priority,
ADD COLUMN customerIdx INT NULL COMMENT '고객 번호 (NULL 허용)' AFTER adminIdx,
ADD COLUMN createdAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시' AFTER hide,
ADD COLUMN updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시' AFTER createdAt;

-- 4. 기존 컬럼 수정
ALTER TABLE center 
MODIFY COLUMN adminIdx INT NULL COMMENT '관리자 번호 (NULL 허용)',
MODIFY COLUMN content TEXT NOT NULL COMMENT '내용',
MODIFY COLUMN status INT DEFAULT 0 COMMENT '상태 (0: 대기, 1: 처리중, 2: 완료)',
MODIFY COLUMN hide TINYINT(1) DEFAULT 0 COMMENT '숨김 여부';

-- 4-1. type 컬럼명을 mainCategory로 변경
ALTER TABLE center CHANGE COLUMN type mainCategory VARCHAR(50) NOT NULL DEFAULT '문의' COMMENT '메인 카테고리 (문의, 신고, 제안 등)';

-- 5. 인덱스 추가
ALTER TABLE center 
ADD INDEX idx_main_category (mainCategory),
ADD INDEX idx_status (status),
ADD INDEX idx_priority (priority),
ADD INDEX idx_sub_category (subCategory),
ADD INDEX idx_customer (customerIdx),
ADD INDEX idx_created_at (createdAt);

-- 6. 새로운 외래키 제약조건 추가
ALTER TABLE center 
ADD CONSTRAINT FK_center_admin 
    FOREIGN KEY (adminIdx) REFERENCES admin(adminIdx) 
    ON DELETE SET NULL ON UPDATE CASCADE,
ADD CONSTRAINT FK_center_customer 
    FOREIGN KEY (customerIdx) REFERENCES customer(customerIdx) 
    ON DELETE SET NULL ON UPDATE CASCADE;

-- 트랜잭션 커밋 (모든 작업이 성공하면 커밋)
COMMIT;
