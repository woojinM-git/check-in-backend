-- ReviewImage 테이블 생성 (리뷰 이미지 저장용 - 2~5장 저장)
-- 1장은 review 테이블의 imageUrl 컬럼에 저장
-- reviewIdx당 한 행만 존재 (1:1 관계)
CREATE TABLE IF NOT EXISTS reviewImage (
    reviewImageIdx INT AUTO_INCREMENT PRIMARY KEY,
    reviewIdx INT NOT NULL UNIQUE COMMENT 'review 테이블과 1:1 관계',
    contentid VARCHAR(50) NOT NULL COMMENT '호텔 ID (호텔별 리뷰 이미지 조회용)',
    imageUrl2 VARCHAR(500) NULL COMMENT '두 번째 이미지 URL',
    imageUrl3 VARCHAR(500) NULL COMMENT '세 번째 이미지 URL',
    imageUrl4 VARCHAR(500) NULL COMMENT '네 번째 이미지 URL',
    imageUrl5 VARCHAR(500) NULL COMMENT '다섯 번째 이미지 URL',
    FOREIGN KEY (reviewIdx) REFERENCES review(reviewIdx) ON DELETE CASCADE,
    INDEX idx_reviewIdx (reviewIdx),
    INDEX idx_contentid (contentid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리뷰 이미지 테이블 (2~5장 저장)';

