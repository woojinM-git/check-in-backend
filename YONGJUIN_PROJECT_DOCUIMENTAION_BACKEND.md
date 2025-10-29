# 🏨 호텔 예약 결제 시스템 - Backend

> **담당자**: yongjun  
> **작업 범위**: 호텔 예약 결제 Backend 구현  
> **작업 기간**: 2025-10-27  
> **개발 목표**: 트랜잭션 보장 및 데이터 무결성 확립

---

## 📁 주요 작업 파일

```
src/main/java/com/sist/backend/
├── service/
│   ├── PaymentService.java          # 결제 검증 및 저장 로직
│   ├── TossPaymentsService.java    # TossPayments API 통신
│   ├── MailService.java            # 이메일 발송 (QR 코드 포함)
│   └── CustomerRepository.java      # Customer 조회 및 업데이트
│
├── controller/
│   └── PaymentController.java       # REST API 엔드포인트
│
├── repository/
│   ├── RoomPaymentRepository.java  # RoomPayment 저장
│   ├── RoomReservationRepository.java # RoomReservation 저장
│   ├── CustomerRepository.java     # Customer 업데이트
│   └── RoomRepository.java         # Room 조회 (복합키)
│
├── entity/
│   ├── Room.java                   # Room 엔티티 (복합키)
│   ├── RoomId.java                # Room 복합키 클래스
│   ├── RoomPayment.java           # 결제 정보
│   ├── RoomReservation.java       # 예약 정보
│   └── Customer.java              # 고객 정보
│
└── dto/
    ├── PaymentRequestDto.java      # 결제 요청 DTO
    └── PaymentResponseDto.java    # 결제 응답 DTO
```

---

## 🎯 주요 기능

### 1️⃣ 결제 검증 및 저장 프로세스

```java
@Transactional(rollbackFor = Exception.class)
public PaymentResponseDto verifyAndSavePayment(PaymentRequestDto request) {
  try {
    // 1단계: TossPayments API로 결제 검증
    Map<String, Object> tossResponse = tossPaymentsService.confirmPayment(
        request.getPaymentKey(),
        request.getOrderId(),
        request.getAmount()
    );

    // 2단계: 검증 결과 확인
    String status = (String) tossResponse.get("status");
    if (!"DONE".equals(status)) {
      throw new RuntimeException("TossPayments 결제 검증 실패");
    }

    // 3단계: RoomPayment 저장
    RoomPayment savedPayment = savePayment(request);

    // 4단계: 호텔 예약 저장 (호텔 예약만)
    if ("hotel_reservation".equals(request.getType()) && request.getContentId() != null) {
      saveRoomReservation(request, savedPayment.getOrderIdx());
    }

    // 5단계: Customer 테이블 업데이트 (캐시/포인트 차감)
    updateCustomerBalance(request);

    return successResponse;
  } catch (Exception e) {
    log.error("결제 처리 실패 - 트랜잭션 롤백", e);
    throw e; // 예외를 다시 던져서 롤백 보장
  }
}
```

#### **주요 특징**

- ✅ **트랜잭션 보장**: 5단계 모두 성공해야만 커밋, 하나라도 실패하면 전체 롤백
- 프론트엔드에서 온 정보를 믿지말자 백엔드에서 한번더 검증하자@@
- ✅ **외래키 검증**: Room 존재 여부 확인 후 RoomReservation 저장
- ✅ **잔액 검증**: Customer의 캐시/포인트 부족 시 예외 발생
- ✅ **비동기 처리**: 이메일 발송은 `@Async`로 분리하여 트랜잭션에 영향 없음

---

### 2️⃣ 복합키 검증 로직

**문제**: `Room` 엔티티는 복합키(`roomIdx` + `contentId`)를 사용

**해결**:

```java
private void saveRoomReservation(PaymentRequestDto request, Integer orderIdx) {
  // Room 존재 여부 확인 (복합 키)
  RoomId roomId = new RoomId(request.getRoomId(), request.getContentId());
  Room room = roomRepository.findById(roomId)
      .orElseThrow(() -> new RuntimeException(
          String.format("객실 정보를 찾을 수 없습니다: roomIdx=%d, contentId=%s",
              request.getRoomId(), request.getContentId())
      ));

  log.info("객실 정보 확인 완료: roomIdx={}, contentId={}, name={}",
      room.getRoomIdx(), room.getContentId(), room.getName());

  RoomReservation reservation = RoomReservation.builder()
      .customerIdx(request.getCustomerIdx())
      .roomIdx(request.getRoomId())
      .contentid(request.getContentId())
      .orderIdx(orderIdx)
      // ...
      .build();

  roomReservationRepository.save(reservation);
}
```

---

### 3️⃣ Customer 잔액 업데이트

```java
private void updateCustomerBalance(PaymentRequestDto request) {
  Customer customer = customerRepository.findById(request.getCustomerIdx())
      .orElseThrow(() -> new RuntimeException("고객 정보를 찾을 수 없습니다"));

  // 캐시 차감
  int usedCash = request.getCashUsed() != null ? request.getCashUsed() : 0;
  if (usedCash > 0 && customer.getCash() != null) {
    if (customer.getCash() < usedCash) {
      throw new RuntimeException("보유 캐시가 부족합니다");
    }
    customer.setCash(customer.getCash() - usedCash);
  }

  // 포인트 차감
  int usedPoint = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
  if (usedPoint > 0 && customer.getPoint() != null) {
    if (customer.getPoint() < usedPoint) {
      throw new RuntimeException("보유 포인트가 부족합니다");
    }
    customer.setPoint(customer.getPoint() - usedPoint);
  }

  customerRepository.save(customer);
}
```

---

## 🐛 트러블 슈팅

### 1️⃣ 외래키 제약조건 오류

**오류 메시지**:

```
Cannot add or update a child row: a foreign key constraint fails
(`checkin`.`roomReservation`, CONSTRAINT `fk_roomReservation_roomIdx`
FOREIGN KEY (`roomIdx`) REFERENCES `room` (`roomIdx`))
```

**원인 분석**:

1. `RoomReservation` 테이블은 `roomIdx`로 `Room`을 참조
2. 하지만 `Room` 테이블은 **복합키**를 사용 (`roomIdx` + `contentId`)
3. 프론트엔드에서 `roomIdx`만 전달하고 `contentId`가 누락됨
4. 또는 전달된 `roomIdx` + `contentId` 조합이 존재하지 않음

**해결 과정**:

1. Room 엔티티 구조 파악 (RoomId 복합키 클래스 확인)
2. RoomRepository로 복합키 조회 구현
3. Room 존재 여부 사전 검증 로직 추가
4. 프론트엔드에서 `contentId` 전달 경로 수정

**핵심 코드**:

```java
RoomId roomId = new RoomId(request.getRoomId(), request.getContentId());
Room room = roomRepository.findById(roomId)
    .orElseThrow(() -> new RuntimeException("객실 정보를 찾을 수 없습니다"));
```

**교훈**:

- DB 스키마를 정확히 파악해야 함
- 복합키를 사용하는 엔티티는 모든 키를 검증해야 함
- 외래키 제약조건 오류는 부모 테이블 데이터 누락/불일치에서 발생

---

### 2️⃣ JSON 파싱 오류

**오류 메시지**:

```
Cannot deserialize value of type `java.lang.Integer` from String "142839-3447"
```

**원인 분석**:

1. `contentId`는 String 타입 ("142839-3447")
2. `roomId`는 Integer 타입
3. 프론트엔드에서 `contentId`를 `roomId`로 파싱 시도

**해결 과정**:

1. DTO 필드 타입 확인
2. 프론트엔드에서 명시적 타입 변환

```javascript
contentId: String(hotelInfo?.contentId || ""),  // String 타입
roomId: parseInt(hotelInfo?.roomIdx || hotelInfo?.roomId), // Integer 타입
```

**교훈**:

- DTO 타입을 정확히 파악하고 명시적으로 변환
- 자동 타입 변환에 의존하지 말 것

---

### 3️⃣ 트랜잭션 롤백 문제

**오류 메시지**:

```
Transaction silently rolled back because it has been marked as rollback-only
```

**원인 분석**:

1. `@Transactional` 내부에서 예외 발생
2. `try-catch`로 예외를 삼켜서 롤백만 되고 에러 메시지 없음
3. 이메일 발송 실패 시 결제도 롤백됨

**해결 과정**:

1. `@Transactional(rollbackFor = Exception.class)` 추가
2. 예외를 다시 던져서 정상적인 롤백 보장

```java
@Transactional(rollbackFor = Exception.class)
public PaymentResponseDto verifyAndSavePayment(PaymentRequestDto request) {
  try {
    // 1-5단계 처리
    return successResponse;
  } catch (Exception e) {
    log.error("결제 처리 실패 - 롤백", e);
    throw e; // 예외를 다시 던져서 롤백 보장
  }
}
```

3. 이메일 발송을 `@Async`로 분리하여 트랜잭션에 영향 없도록

```java
@Async
private void sendEmailAsync(PaymentRequestDto request, String qrUrl) {
  // 이메일 발송
}
```

**교훈**:

- 트랜잭션 내부에서는 예외를 절대 삼키지 말 것
- `rollbackFor = Exception.class` 옵션 사용
- 비동기 작업은 트랜잭션 밖으로 분리

---

### 4️⃣ Customer 잔액 업데이트 누락

**문제**: `cashUsed` 컬럼이 `RoomPayment`에 없음

**원인 분석**:

1. 처음엔 `RoomPayment`에 `cashUsed` 필드를 추가하려고 시도
2. 실제로는 `Customer` 테이블의 `cash`와 `point`를 차감해야 함

**해결 과정**:

1. DB 스키마 재확인
2. `updateCustomerBalance` 메서드 구현
3. 잔액 부족 시 예외 발생하도록 구현
4. 트랜잭션 내에서 원자적으로 처리

**교훈**:

- DB 스키마를 정확히 파악하고 변경에 신중해야 함
- 잔액 확인 로직 필수
- 트랜잭션 내에서 모든 업데이트가 원자적으로 처리되어야 함

---

## 🚀 향후 개선 사항

### 단기 (1-2주)

1. **예약 가능 여부 사전 체크**

   - [ ] 동일 기간 중복 예약 체크
   - [ ] 객실 재고 확인
   - [ ] 체크인/체크아웃 날짜 유효성 검증 강화

2. **로깅 강화**

   - [ ] 결제 프로세스 각 단계별 상세 로그
   - [ ] 에러 발생 시 스택 트레이스 기록
   - [ ] 결제 성공률 대시보드

3. **테스트 코드 작성**
   - [ ] 단위 테스트 (각 메서드별)
   - [ ] 통합 테스트 (전체 흐름)
   - [ ] Edge case 테스트

---

### 중기 (1개월)

1. **결제 방식 확장**

   - [ ] 계좌이체 결제
   - [ ] 가상계좌 결제
   - [ ] 간편결제 (페이코, 네이버페이)

2. **동시성 처리**

   - [ ] 동시 예약 요청 시 Lock 처리
   - [ ] Redis 분산 락 적용
   - [ ] DB 락 최적화

3. **알림 시스템**
   - [ ] 예약 확인 SMS
   - [ ] 결제 완료 Push 알림
   - [ ] 예약 취소 알림

---

### 장기 (2개월+)

1. **모니터링 및 알림**

   - [ ] Spring Actuator로 메트릭 수집
   - [ ] Sentry 에러 추적 연동
   - [ ] Datadog APM
   - [ ] 결제 실패율 감지 시 알림

2. **보안 강화**

   - [ ] CSRF 방지
   - [ ] Rate Limiting (결제 API)
   - [ ] 결제 금액 검증 (클라이언트 조작 방지)
   - [ ] JWT 토큰 refresh

3. **성능 최적화**
   - [ ] N+1 쿼리 문제 해결
   - [ ] 캐싱 전략 (Redis)
   - [ ] DB 인덱스 최적화

---

## 💼 개발자 포트폴리오 강점

### 1. 트랜잭션 및 데이터 무결성

**도전**: 결제, 예약, 고객 잔액 업데이트의 원자성 보장

**해결**:

- `@Transactional(rollbackFor = Exception.class)` 사용
- 모든 예외를 re-throw하여 롤백 보장
- 5단계 검증 프로세스 구현
- 이메일 발송은 비동기로 분리하여 트랜잭션 영향 없음

**역량**:

- ✅ ACID 원칙 이해
- ✅ 분산 시스템에서의 일관성 보장
- ✅ 실패 처리 전략 수립

---

### 2. 복잡한 문제 해결 능력

**트러블 슈팅 경험**:

- 외래키 제약조건 오류 → 복합키 검증 로직 구현
- JSON 파싱 오류 → 명시적 타입 변환
- 트랜잭션 롤백 문제 → 예외 처리 전략 수립
- Customer 잔액 업데이트 누락 → 트랜잭션 내 원자적 처리

**역량**:

- ✅ 근본 원인 파악 능력
- ✅ 체계적인 문제 해결 과정
- ✅ 문서화 습관

---

### 3. 외부 API 통합

**TossPayments 연동**:

- 결제 검증 API 호출
- 결제 키, 주문 ID, 금액 검증
- 에러 처리 및 재시도 로직

**역량**:

- ✅ RESTful API 통합 경험
- ✅ 외부 서비스 연동 능력
- ✅ 에러 핸들링

---

## 📝 핵심 코드 예시

### PaymentService.java

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final RoomPaymentRepository roomPaymentRepository;
  private final RoomReservationRepository roomReservationRepository;
  private final TossPaymentsService tossPaymentsService;
  private final MailService mailService;
  private final QRCodeGenerator qrCodeGenerator;
  private final CustomerRepository customerRepository;
  private final RoomRepository roomRepository;

  @Transactional(rollbackFor = Exception.class)
  public PaymentResponseDto verifyAndSavePayment(PaymentRequestDto request) {
    log.info("결제 검증 시작: orderId={}, amount={}",
        request.getOrderId(), request.getAmount());

    try {
      // 1단계: TossPayments 검증
      Map<String, Object> tossResponse = tossPaymentsService.confirmPayment(
          request.getPaymentKey(),
          request.getOrderId(),
          request.getAmount()
      );

      // 2단계: 검증 결과 확인
      String status = (String) tossResponse.get("status");
      if (!"DONE".equals(status)) {
        throw new RuntimeException("TossPayments 결제 검증 실패: status=" + status);
      }

      // 3단계: DB 저장
      RoomPayment savedPayment = savePayment(request);

      // 4단계: 호텔 예약 저장
      if ("hotel_reservation".equals(request.getType()) && request.getContentId() != null) {
        saveRoomReservation(request, savedPayment.getOrderIdx());
      }

      // 5단계: Customer 잔액 업데이트
      updateCustomerBalance(request);

      log.info("결제 및 예약 정보 저장 완료: orderIdx={}", savedPayment.getOrderIdx());

      return PaymentResponseDto.builder()
          .success(true)
          .message("결제가 성공적으로 완료되었습니다.")
          .orderId(request.getOrderId())
          .paymentKey(request.getPaymentKey())
          .amount(request.getAmount())
          .status("DONE")
          .approvedAt(savedPayment.getApprovedAt())
          .receiptUrl(savedPayment.getReceiptUrl())
          .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
          .emailSent(false)
          .build();

    } catch (RuntimeException e) {
      log.error("결제 처리 실패 - 트랜잭션 롤백: orderId={}",
          request.getOrderId(), e);
      throw e;
    } catch (Exception e) {
      log.error("결제 처리 실패 - 트랜잭션 롤백: orderId={}",
          request.getOrderId(), e);
      throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  private void saveRoomReservation(PaymentRequestDto request, Integer orderIdx) {
    // Room 존재 여부 확인 (복합 키)
    RoomId roomId = new RoomId(request.getRoomId(), request.getContentId());
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new RuntimeException(
            String.format("객실 정보를 찾을 수 없습니다: roomIdx=%d, contentId=%s",
                request.getRoomId(), request.getContentId())
        ));

    log.info("객실 정보 확인 완료: roomIdx={}, contentId={}, name={}",
        room.getRoomIdx(), room.getContentId(), room.getName());

    RoomReservation reservation = RoomReservation.builder()
        .customerIdx(request.getCustomerIdx())
        .roomIdx(request.getRoomId())
        .contentid(request.getContentId())
        .orderIdx(orderIdx)
        .checkinDate(request.getCheckIn() != null ? LocalDate.parse(request.getCheckIn()) : null)
        .checkoutDate(request.getCheckOut() != null ? LocalDate.parse(request.getCheckOut()) : null)
        .guest(request.getGuests())
        .totalPrice(request.getTotalPrice())
        .status(1)
        .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    roomReservationRepository.save(reservation);
    log.info("예약 정보 저장 완료: reservIdx={}", reservation.getReservIdx());
  }

  private void updateCustomerBalance(PaymentRequestDto request) {
    try {
      Customer customer = customerRepository.findById(request.getCustomerIdx())
          .orElseThrow(() -> new RuntimeException("고객 정보를 찾을 수 없습니다"));

      int usedCash = request.getCashUsed() != null ? request.getCashUsed() : 0;
      if (usedCash > 0 && customer.getCash() != null) {
        if (customer.getCash() < usedCash) {
          throw new RuntimeException("보유 캐시가 부족합니다");
        }
        customer.setCash(customer.getCash() - usedCash);
      }

      int usedPoint = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
      if (usedPoint > 0 && customer.getPoint() != null) {
        if (customer.getPoint() < usedPoint) {
          throw new RuntimeException("보유 포인트가 부족합니다");
        }
        customer.setPoint(customer.getPoint() - usedPoint);
      }

      customerRepository.save(customer);
      log.info("고객 잔액 업데이트 완료: customerIdx={}", request.getCustomerIdx());

    } catch (Exception e) {
      log.error("고객 잔액 업데이트 실패", e);
      throw new RuntimeException("고객 잔액 업데이트 중 오류가 발생했습니다: " + e.getMessage());
    }
  }
}
```

---

## 🔐 보안 고려사항

### 구현 완료

1. **트랜잭션 롤백**: 실패 시 모든 DB 변경사항 롤백
2. **결제 검증**: TossPayments API로 반드시 검증
3. **잔액 검증**: Customer 잔액 부족 시 예외 발생
4. **외래키 검증**: Room 존재 여부 사전 확인

### 향후 보완

- [ ] CSRF 토큰 검증
- [ ] Rate Limiting (결제 API)
- [ ] 결제 금액 검증 (클라이언트 조작 방지)
- [ ] JWT 토큰 refresh

---

## 📊 성과 지표

### 구현 완료

- ✅ 결제부터 DB 저장까지 완전 자동화
- ✅ 트랜잭션 안전성 보장 (롤백 지원)
- ✅ 외래키 제약조건 해결
- ✅ Customer 잔액 실시간 차감
- ✅ 5단계 검증 프로세스 구현

### 코드 품질

- **트랜잭션 관리**: `@Transactional` 옵션 최적화
- **에러 핸들링**: 모든 예외를 로깅 및 re-throw
- **로깅**: 각 단계별 상세 로그
- **모듈화**: private 메서드로 기능 분리

---

## 📖 참고 자료

- [Spring Transaction 관리](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [JPA 복합 키](https://docs.jboss.org/hibernate/orm/5.3/userguide/html_single/Hibernate_User_Guide.html#composite-identifiers)
- [TossPayments 결제 API](https://docs.tosspayments.com/reference)
- [Spring Boot Async](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)

---

## 📞 문의

- **담당자**: yongjun
- **영역**: 호텔 예약 결제 Backend
- **충돌 시**: PR/코멘트로 전달

---

_Last Updated: 2025-10-27_
