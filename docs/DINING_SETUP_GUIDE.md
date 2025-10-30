# 다이닝 설정 가이드

## 현재 상황

- **호텔 카테고리 업소**: 총 617개
- **다이닝 데이터**: 0개 (현재 없음)
- **문제**: 호텔 카테고리 업소에 대한 다이닝 정보가 없어 예약 불가

## Flyway 마이그레이션 (V43)

### 실행 방법

```bash
# 백엔드 재시작 시 자동으로 실행됩니다
cd backend
./gradlew bootRun
```

### V43 마이그레이션 내용

`V43__insert_default_dining_for_hotels.sql`

- **대상**: 호텔 카테고리(B02010100)의 모든 업소
- **생성 데이터**: 각 호텔마다 기본 레스토랑 1개 자동 생성
- **기본 설정**:
  - 이름: `{호텔명} 레스토랑`
  - 좌석: 50석
  - 가격: 30,000원/1인
  - 영업시간: 07:00 ~ 22:00
  - 예약 단위: 60분
  - 상태: 활성(1)

### 예시

```sql
-- 다우리조텔 → "다우리조텔 레스토랑"
-- 반얀트리 클럽 앤 스파 서울 → "반얀트리 클럽 앤 스파 서울 레스토랑"
```

## 향후 개선 방안

### 1. 관리자 호텔 등록 시 다이닝 정보 포함

#### 현재 프로세스
```
1. 관리자가 HotelDraft에 호텔 정보 임시 저장 (JSON)
2. RegistrationRequest 생성 (등록 요청)
3. Master가 승인/거부
4. 승인 시 status만 업데이트 (실제 HotelInfo 생성 X)
```

#### 문제점
- HotelDraft의 formData(JSON)에 다이닝 정보가 포함되지 않음
- 승인 시 실제 HotelInfo와 Dining을 생성하는 로직이 없음

#### 개선 방향

**1단계: HotelDraft JSON 구조 확장**

```json
{
  "hotelInfo": {
    "title": "호텔명",
    "adress": "주소",
    "tel": "전화번호",
    ...
  },
  "rooms": [...],
  "dinings": [
    {
      "name": "레스토랑명",
      "description": "설명",
      "totalSeats": 50,
      "basePrice": 30000,
      "openTime": "07:00",
      "closeTime": "22:00",
      "slotDuration": 60,
      "maxGuestsPerSlot": 50
    }
  ]
}
```

**2단계: 승인 프로세스 개선**

```java
@Transactional
public void approveHotelRegistration(Integer registrationIdx) {
    // 1. RegistrationRequest 조회
    RegistrationRequest request = findById(registrationIdx);
    
    // 2. HotelDraft의 formData 파싱
    HotelDraft draft = hotelDraftService.findById(request.getDraftIdx());
    Map<String, Object> formData = parseFormData(draft.getFormData());
    
    // 3. HotelInfo 생성
    HotelInfo hotelInfo = createHotelInfo(formData);
    hotelInfoRepository.save(hotelInfo);
    
    // 4. Dining 생성 (formData에 dinings가 있으면)
    if (formData.containsKey("dinings")) {
        List<Map<String, Object>> dinings = (List) formData.get("dinings");
        for (Map<String, Object> diningData : dinings) {
            Dining dining = createDining(hotelInfo.getContentId(), diningData);
            diningRepository.save(dining);
        }
    }
    
    // 5. 승인 상태 업데이트
    updateRequest(registrationIdx, 1, LocalDateTime.now());
}
```

**3단계: 프론트엔드 폼 개선**

- 호텔 등록 폼에 다이닝 정보 입력 섹션 추가
- 다이닝 정보를 배열로 관리 (여러 레스토랑 등록 가능)
- 필수 항목: 이름, 좌석 수, 가격, 영업시간

### 2. 파일 위치

```
backend/
├── src/main/resources/db/migration/
│   └── V43__insert_default_dining_for_hotels.sql  ✅ 생성 완료
├── docs/
│   └── DINING_SETUP_GUIDE.md  ✅ 현재 파일
└── src/main/java/com/sist/backend/
    ├── controller/HotelDraftController.java  📝 개선 필요
    ├── controller/MasterManagementController.java  📝 개선 필요
    └── service/RegistrationRequestService.java  📝 개선 필요
```

## 실행 확인

### 마이그레이션 실행 확인

```sql
-- Flyway 이력 확인
SELECT * FROM flyway_schema_history 
WHERE version = '43' 
ORDER BY installed_on DESC;

-- 생성된 다이닝 개수 확인
SELECT COUNT(*) as total_dinings FROM dining;

-- 호텔별 다이닝 확인
SELECT h.contentId, h.title, d.name as dining_name
FROM hotelInfo h
INNER JOIN category c ON h.hotelCategoryCode = c.hotelCategoryCode
LEFT JOIN dining d ON h.contentId = d.contentid
WHERE c.categoryName = '호텔'
LIMIT 10;
```

### 예상 결과

```
total_dinings: 617
```

## 참고 사항

- **자동 생성된 데이터는 기본값**: 실제 운영 전에 각 호텔별로 정확한 다이닝 정보로 업데이트 필요
- **호텔 카테고리만 대상**: 리조트, 펜션 등은 포함하지 않음
- **중복 방지**: 이미 다이닝이 있는 호텔은 제외 (NOT EXISTS 조건)

## 다음 단계

1. ✅ V43 마이그레이션 실행 (백엔드 재시작)
2. ⬜ 프론트엔드 호텔 등록 폼에 다이닝 섹션 추가
3. ⬜ 승인 프로세스에서 HotelInfo + Dining 생성 로직 구현
4. ⬜ 기존 호텔의 다이닝 정보 수정 기능 추가

