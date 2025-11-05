-- Review 테이블에 imageUrl 컬럼 추가 (첫 번째 이미지 저장용)
ALTER TABLE review ADD COLUMN imageUrl VARCHAR(500) NULL COMMENT 'S3 이미지 URL (첫 번째 이미지)';

-- 인덱스 추가 (이미지가 있는 리뷰 조회 최적화)
-- MySQL은 부분 인덱스를 지원하지 않으므로 일반 인덱스로 생성
CREATE INDEX idx_review_imageUrl ON review(imageUrl);

