-- =====================================================
-- V33: 다이닝 샘플 데이터 추가
-- =====================================================

-- 참고: 실제 호텔 contentid를 사용하여 샘플 데이터 추가
-- contentid가 존재하지 않으면 FK 제약조건 위반으로 INSERT 실패
-- 필요시 이 파일을 수정하여 실제 호텔 ID를 사용하거나 주석 처리

-- 실제 존재하는 호텔 ID 확인 (첫 번째 호텔 사용)
SET @first_hotel_id = (SELECT contentId FROM hotelInfo LIMIT 1);

-- 1. 조식 뷔페 (첫 번째 호텔에 추가)
INSERT INTO dining (
    contentid, 
    name, 
    description, 
    imageUrl,
    totalSeats, 
    basePrice, 
    openTime, 
    closeTime, 
    slotDuration, 
    maxGuestsPerSlot, 
    status,
    content,
    -- deprecated fields (호환성 유지)
    time,
    price,
    totalCount,
    date
) VALUES 
-- 조식 뷔페
(
    @first_hotel_id, 
    '조식 뷔페',
    '신선한 재료로 준비한 다양한 조식 메뉴를 뷔페로 즐기실 수 있습니다',
    'https://example.com/breakfast.jpg',
    50,        -- totalSeats
    25000,     -- basePrice (1인)
    '07:00:00',
    '10:00:00',
    30,        -- 30분 단위 예약
    50,        -- 시간대별 최대 50명
    1,         -- 활성
    '조식 뷔페 포함: 샐러드바, 따뜻한 요리, 빵 & 베이커리, 음료',
    -- deprecated
    '07:00-10:00',
    25000,
    50,
    CURDATE()
),

-- 석식 코스
(
    @first_hotel_id,
    '석식 코스 A',
    '셰프 특선 3코스 디너 - 전채, 메인, 디저트',
    'https://example.com/dinner-a.jpg',
    30,
    85000,
    '18:00:00',
    '22:00:00',
    60,        -- 60분 단위 예약
    30,
    1,
    '코스 구성: 전채 요리, 수프, 메인 요리(스테이크 or 해산물), 디저트, 와인 1잔',
    -- deprecated
    '18:00-22:00',
    85000,
    30,
    CURDATE()
),

-- 라운지 티타임
(
    @first_hotel_id,
    '라운지 티타임',
    '호텔 라운지에서 즐기는 애프터눈 티 세트',
    'https://example.com/tea-time.jpg',
    20,
    38000,
    '14:00:00',
    '17:00:00',
    30,
    20,
    1,
    '티타임 세트: 스콘, 핑거 샌드위치, 미니 디저트 3종, 프리미엄 차 또는 커피',
    -- deprecated
    '14:00-17:00',
    38000,
    20,
    CURDATE()
);

-- 참고: 다른 호텔에도 다이닝을 추가하려면 아래와 같이 추가
-- INSERT INTO dining (contentid, name, ...) VALUES ('호텔ID', '다이닝명', ...);

