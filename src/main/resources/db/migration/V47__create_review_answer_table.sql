-- Create table for review answers by business owners
-- 사업자 리뷰 답변 테이블 생성

CREATE TABLE IF NOT EXISTS reviewAnswer (
    reviewAnswerIdx INT AUTO_INCREMENT PRIMARY KEY COMMENT '답변 고유번호',
    reviewIdx INT NOT NULL COMMENT '리뷰 고유번호',
    adminIdx INT NOT NULL COMMENT '사업자 고유번호',
    content TEXT NOT NULL COMMENT '답변 내용',
    status INT NOT NULL DEFAULT 1 COMMENT '상태 (1: 활성, 0: 비활성)',
    createdAt DATETIME NOT NULL COMMENT '작성일시',
    updatedAt DATETIME NOT NULL COMMENT '수정일시',
    
    FOREIGN KEY (reviewIdx) REFERENCES review(reviewIdx) ON DELETE CASCADE,
    FOREIGN KEY (adminIdx) REFERENCES admin(adminIdx) ON DELETE CASCADE,
    
    -- 한 리뷰당 하나의 답변만 허용
    UNIQUE KEY unique_review_answer (reviewIdx),
    
    -- 인덱스
    INDEX idx_adminIdx (adminIdx),
    INDEX idx_status (status),
    INDEX idx_createdAt (createdAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 답변';

