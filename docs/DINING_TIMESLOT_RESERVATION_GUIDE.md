# 다이닝 시간대별 예약 시스템 가이드

## 📋 개요

호텔 다이닝(조식, 석식, 티타임 등)을 시간대별로 예약할 수 있는 시스템으로 개선했습니다.

## 🔄 마이그레이션 내역

### V30: dining 테이블 구조 개선
- **새로운 컬럼 추가**:
  - `name`: 다이닝 이름 (예: 조식 뷔페)
  - `description`: 설명
  - `imageUrl`: 이미지 URL
  - `totalSeats`: 총 좌석 수
  - `basePrice`: 1인당 기본 가격
  - `openTime`: 오픈 시간
  - `closeTime`: 마감 시간
  - `slotDuration`: 예약 시간 단위 (분)
  - `maxGuestsPerSlot`: 시간대별 최대 인원
  - `status`: 활성/비활성
  - `createdAt`, `updatedAt`: 타임스탬프

- **Deprecated 필드**: `time`, `price`, `date`, `totalCount`, `bookedCount`
  - 호환성을 위해 유지하지만 사용하지 않음

### V31: diningReservation 테이블 구조 개선
- **새로운 컬럼 추가**:
  - `reservationDate`: 예약 날짜 (LocalDate)
  - `reservationTime`: 예약 시간 (LocalTime)
  - `guest`: 인원 수
  - `totalPrice`: 총 가격
  - `qrUrl`: QR코드 URL (입장 확인용)
  - `specialRequest`: 특별 요청사항
  - `createdAt`, `updatedAt`: 타임스탬프

- **타입 변경**:
  - `status`: VARCHAR → INT (0:대기, 1:확정, 2:취소, 3:노쇼, 4:완료)

- **Deprecated 필드**: `checkIn`
  - `reservationDate`, `reservationTime`으로 분리

- **인덱스 추가**: 성능 최적화
  - `idx_dining_reservation_date_time`
  - `idx_dining_reservation_customer`
  - `idx_dining_reservation_status`

### V32: diningPayment 테이블 구조 개선
- **인덱스 추가**: 성능 최적화
  - `idx_dining_payment_customer`
  - `idx_dining_payment_status`
  - `idx_dining_payment_key`
  - `idx_dining_payment_dining`

### V33: 샘플 데이터 추가
- 베리온리조트 다이닝 상품 3개
  - 조식 뷔페 (07:00-10:00, 30분 단위, 50명)
  - 석식 코스 A (18:00-22:00, 60분 단위, 30명)
  - 라운지 티타임 (14:00-17:00, 30분 단위, 20명)

## 📊 테이블 구조

### dining (다이닝 상품 정보)
```sql
diningIdx           INT (PK, AUTO_INCREMENT)
contentid           VARCHAR(50) (FK -> hotelInfo)
name                VARCHAR(100)         -- 다이닝 이름
description         TEXT                 -- 설명
imageUrl            VARCHAR(500)         -- 이미지
totalSeats          INT                  -- 총 좌석 수
basePrice           INT                  -- 1인당 가격
openTime            TIME                 -- 오픈 시간
closeTime           TIME                 -- 마감 시간
slotDuration        INT                  -- 예약 시간 단위(분)
maxGuestsPerSlot    INT                  -- 시간대별 최대 인원
status              INT                  -- 0:비활성, 1:활성
content             TEXT                 -- 상세 정보
createdAt           DATETIME
updatedAt           DATETIME
```

### diningResrevation (예약 정보)
```sql
diningResrIdx       INT (PK, AUTO_INCREMENT)
diningIdx           INT (FK -> dining)
customerIdx         INT (FK -> customer)
diningpayIdx        INT (FK -> diningPayment)
reservationDate     DATE                 -- 예약 날짜
reservationTime     TIME                 -- 예약 시간
guest               INT                  -- 인원 수
totalPrice          INT                  -- 총 가격
status              INT                  -- 0:대기, 1:확정, 2:취소, 3:노쇼, 4:완료
qrUrl               VARCHAR(500)         -- QR코드
specialRequest      TEXT                 -- 특별 요청
createdAt           DATETIME
updatedAt           DATETIME
```

### diningPayment (결제 정보)
```sql
diningpayIdx        INT (PK, AUTO_INCREMENT)
diningIdx           INT (FK -> dining)
customerIdx         INT (FK -> customer)
couponIdx           INT                  -- 쿠폰 ID
price               INT                  -- 결제 금액
status              INT                  -- 0:대기, 1:완료, 2:취소, 3:환불
paymentKey          VARCHAR(50)          -- 토스페이먼츠 키
pointUsed           INT                  -- 사용 포인트
method              VARCHAR(255)         -- 결제 수단
receiptUrl          VARCHAR(500)         -- 영수증 URL
createdAt           DATETIME
approvedAt          DATETIME
updatedAt           DATETIME
```

## 🔑 핵심 개념

### 시간대(Timeslot) 관리
- 다이닝의 운영 시간을 `slotDuration`(분) 단위로 분할
- 예: 07:00-10:00 운영, 30분 단위 → 07:00, 07:30, 08:00, 08:30, 09:00, 09:30

### 재고 관리
- `maxGuestsPerSlot`: 각 시간대별 최대 수용 인원
- `bookedCount` 컬럼 제거: 쿼리로 실시간 집계 (동시성 문제 해결)

## 🎯 사용 예시

### 1. 다이닝 상품 조회
```java
GET /api/dining?contentId=2875963

Response:
[
  {
    "diningIdx": 1,
    "name": "조식 뷔페",
    "basePrice": 25000,
    "openTime": "07:00",
    "closeTime": "10:00",
    "slotDuration": 30,
    "maxGuestsPerSlot": 50
  }
]
```

### 2. 특정 날짜의 시간대별 예약 현황
```java
GET /api/dining/1/availability?date=2025-10-24

Response:
[
  {"time": "07:00", "bookedGuests": 45, "available": 5},
  {"time": "07:30", "bookedGuests": 50, "available": 0},
  {"time": "08:00", "bookedGuests": 32, "available": 18},
  ...
]
```

### 3. 예약 생성
```java
POST /api/dining/reservation

Request:
{
  "diningIdx": 1,
  "customerIdx": 105,
  "reservationDate": "2025-10-24",
  "reservationTime": "08:00",
  "guestCount": 4,
  "specialRequest": "창가 자리 부탁드립니다"
}

Response:
{
  "reservationId": 123,
  "status": "confirmed",
  "qrUrl": "https://qr.example.com/dr123",
  "totalPrice": 100000
}
```

## 🔒 동시성 제어

### Repository 쿼리
```java
@Query("SELECT COALESCE(SUM(dr.guest), 0) FROM DiningReservation dr " +
       "WHERE dr.diningIdx = :diningIdx " +
       "AND dr.reservationDate = :date " +
       "AND dr.reservationTime = :time " +
       "AND dr.status IN (0, 1)")
Integer countGuestsByDateAndTime(...);
```

### Service 로직
```java
@Transactional
public DiningReservation createReservation(dto) {
    // 1. 트랜잭션 내에서 재고 확인
    Integer bookedGuests = repository.countGuestsByDateAndTime(...);
    
    // 2. 좌석 부족 체크
    if (bookedGuests + dto.getGuestCount() > dining.getMaxGuestsPerSlot()) {
        throw new RuntimeException("해당 시간대는 예약이 마감되었습니다");
    }
    
    // 3. 예약 생성
    return repository.save(reservation);
}
```

## 📝 마이그레이션 실행 방법

### 1. Flyway 자동 실행
```bash
# 백엔드 서버 시작 시 자동으로 마이그레이션 실행됨
cd backend
./gradlew bootRun
```

### 2. Flyway 수동 실행
```bash
./gradlew flywayMigrate
```

### 3. 마이그레이션 상태 확인
```bash
./gradlew flywayInfo
```

### 4. 마이그레이션 롤백 (필요 시)
```bash
# 특정 버전으로 복원
./gradlew flywayUndo -Pflyway.target=V29
```

## ⚠️ 주의사항

### FK 제약조건
- 기존 FK는 모두 유지됩니다
- `dining.contentid` → `hotelInfo.contentId`
- `diningReservation` → `dining`, `customer`, `diningPayment`
- `diningPayment` → `dining`, `customer`

### 하위 호환성
- Deprecated 필드는 삭제하지 않고 유지
- 기존 코드가 있다면 점진적으로 마이그레이션 가능

### 샘플 데이터
- V33의 샘플 데이터는 `contentid='2875963'`(베리온리조트) 기준
- 실제 환경에서는 존재하는 호텔 ID로 변경 필요

## 🚀 다음 단계

1. **Repository 생성**
   - `DiningRepository`
   - `DiningReservationRepository`
   - `DiningPaymentRepository`

2. **Service 구현**
   - `DiningService`: 다이닝 상품 관리
   - `DiningReservationService`: 예약 관리 (동시성 제어 포함)
   - `DiningPaymentService`: 결제 처리

3. **Controller 구현**
   - `DiningController`: REST API 엔드포인트

4. **프론트엔드 구현**
   - 다이닝 목록 페이지
   - 시간대별 예약 캘린더
   - 예약 확인 페이지 (QR 코드 표시)

## 📚 참고

- 객실 예약 시스템(`RoomReservation`)과 유사한 구조
- 토스페이먼츠 결제 연동 가능
- QR코드 생성/검증 시스템 재사용 가능

