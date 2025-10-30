-- V43: 호텔 카테고리 업소에 기본 다이닝 데이터 삽입
-- 호텔(B02010100) 카테고리의 모든 업소에 대해 기본 다이닝 레스토랑 생성
-- 이후에는 관리자가 호텔 등록 시 직접 다이닝 정보를 입력하도록 처리

INSERT INTO dining (contentid, name, description, imageUrl, totalSeats, basePrice, openTime, closeTime, slotDuration, maxGuestsPerSlot, status, createdAt, updatedAt)
SELECT 
    h.contentId,
    CONCAT(h.title, ' 레스토랑') as name,
    '호텔 내 레스토랑입니다. 조식, 중식, 석식을 제공합니다.' as description,
    NULL as imageUrl,
    50 as totalSeats,
    30000 as basePrice,
    '07:00:00' as openTime,
    '22:00:00' as closeTime,
    60 as slotDuration,
    50 as maxGuestsPerSlot,
    1 as status,
    NOW() as createdAt,
    NOW() as updatedAt
FROM hotelInfo h
INNER JOIN category c ON h.hotelCategoryCode = c.hotelCategoryCode
WHERE c.categoryName = '호텔'
AND NOT EXISTS (
    SELECT 1 FROM dining d WHERE d.contentid = h.contentId
);

-- 삽입된 레코드 수 확인 (로그용)
-- SELECT COUNT(*) as inserted_count FROM dining WHERE name LIKE '% 레스토랑';

