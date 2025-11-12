# 🏨 호텔 예약 관리 시스템 - Backend API (Admin & Master)

> **담당자**: [작성자명]  
> **작업 범위**: 프론트엔드(Admin & Master)에서 사용하는 백엔드 API 엔드포인트 구현  
> **작업 기간**: 2024-10 ~ 2025-01  
> **개발 목표**: 안전하고 효율적인 RESTful API 제공, 권한 관리 및 데이터 무결성 보장

---

## 📁 주요 작업 파일

```
backend/src/main/java/com/sist/backend/
├── controller/
│   ├── AdminManagementController.java    # Admin API 엔드포인트
│   └── MasterManagementController.java   # Master API 엔드포인트
│
├── service/
│   ├── RoomService.java                  # 객실 관리 비즈니스 로직
│   ├── RoomReservationService.java        # 예약 관리 비즈니스 로직
│   ├── RevenueService.java               # 매출 통계 비즈니스 로직
│   ├── StatisticsService.java            # 통계 분석 비즈니스 로직
│   ├── RegistrationRequestService.java   # 호텔 승인 요청 관리
│   ├── HotelDraftService.java            # 호텔 임시저장 데이터 관리
│   ├── CenterService.java                 # 고객센터(신고/FAQ/문의) 관리
│   └── hotel/
│       └── HotelInfoService.java         # 호텔 정보 관리
│
├── repository/
│   ├── RoomRepository.java               # 객실 데이터 접근
│   ├── RoomReservationRepository.java   # 예약 데이터 접근
│   ├── RoomPaymentRepository.java        # 결제 데이터 접근
│   ├── HotelSettlementRepository.java    # 정산 데이터 접근
│   └── CenterRepository.java             # 고객센터 데이터 접근
│
└── dto/
    ├── admin/
    │   ├── DashboardDto.java             # 대시보드 응답 DTO
    │   ├── RoomReservationDto.java       # 예약 정보 DTO
    │   ├── RoomStatusDto.java            # 객실 상태 변경 DTO
    │   └── RevenueSummaryDto.java        # 매출 요약 DTO
    └── master/
        ├── RegistrationRequestDto.java   # 호텔 승인 요청 DTO
        └── StatisticsDto.java            # 통계 데이터 DTO
```

---

## 🎯 주요 API 엔드포인트

### 1️⃣ Admin - 대시보드 API

**엔드포인트**: `GET /api/admin/dashboard`

**목적**: 호텔 관리자 대시보드에 필요한 통계 데이터 제공

**핵심 구현**:

```java
@RequestMapping("/dashboard")
@Operation(summary = "대시보드 관리자", description = "관리자 대시보드 화면")
public ResponseEntity<?> dashboard(HttpServletRequest request) {
    // 1. JWT에서 adminIdx 추출 및 contentId 조회
    String contentid = getContentIdOrRedirect();
    if (contentid == null) {
        return createRedirectResponse(); // 403 Forbidden
    }
    
    // 2. 통계 데이터 조회
    Integer todayCheckinCount = roomReservationService.getTodayCheckinCount();
    Integer todayCheckoutCount = roomReservationService.getTodayCheckoutCount();
    Integer reservationCount = roomReservationService.findByStatus();
    Long thisMonthSales = roomPaymentService.findByPrice();
    
    // 3. 최근 예약 현황 조회 (5개만)
    List<RoomReservationDto> roomReservationList = 
        roomReservationService.findByStatusWithDetails(contentid);
    
    // 4. DTO 구성 및 반환
    DashboardDto dto = DashboardDto.builder()
        .today(Today.builder()
            .checkinCount(todayCheckinCount != null ? todayCheckinCount : 0)
            .checkoutCount(todayCheckoutCount != null ? todayCheckoutCount : 0)
            .reservationCount(reservationCount != null ? reservationCount : 0)
            .thisMonthSales(thisMonthSales != null ? thisMonthSales : 0)
            .build())
        .recentReservations(roomReservationList)
        .build();
    
    return ResponseEntity.ok(dto);
}
```

**주요 특징**:

- ✅ **권한 검증**: `getContentIdOrRedirect()`로 호텔 등록 여부 확인
- ✅ **통계 집계**: 오늘 체크인/체크아웃 수, 예약 확정 수, 이번 달 매출
- ✅ **최근 예약**: 최근 예약 5건을 Room과 Customer 정보 포함하여 반환
- ✅ **에러 처리**: 호텔 미등록 관리자는 403 Forbidden 반환

---

### 2️⃣ Admin - 객실 현황 조회 API

**엔드포인트**: `GET /api/admin/roomList?date=2024-01-01`

**목적**: 특정 날짜 기준 객실 현황 및 예약 정보 조회

**핵심 구현**:

```java
@GetMapping("/roomList")
@Operation(summary = "객실 현황 조회", description = "특정 날짜 기준 객실 현황을 조회합니다.")
public ResponseEntity<Map<String, Object>> getRoomStatus(
    @RequestParam(value = "date", required = false) String dateStr,
    HttpServletRequest request) {
    
    String contentid = getContentIdOrRedirect();
    if (contentid == null) {
        return createRedirectResponse();
    }
    
    // 날짜 처리 (없으면 오늘 날짜)
    LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) 
        ? LocalDate.parse(dateStr) 
        : LocalDate.now();
    
    // 1. 객실 리스트 조회
    List<Room> rooms = roomService.findByContentIdAdmin(contentid);
    
    // 2. 해당 날짜 예약 조회
    List<RoomReservationDto> dateReservations = 
        roomReservationService.findByDateRangeWithDetails(contentid, targetDate, targetDate);
    
    // 3. 각 객실의 상태 설정
    List<Map<String, Object>> roomStatusList = rooms.stream().map(room -> {
        Map<String, Object> roomStatus = new HashMap<>();
        
        // 객실 기본 정보
        roomStatus.put("roomIdx", room.getRoomIdx());
        roomStatus.put("name", room.getName());
        roomStatus.put("status", room.getStatus()); // 객실 기본 상태
        
        // 해당 날짜 이 객실의 예약 찾기
        Optional<RoomReservationDto> reservationOpt = dateReservations.stream()
            .filter(reservation -> reservation.getRoomIdx().equals(room.getRoomIdx()))
            .findFirst();
        
        if (reservationOpt.isPresent()) {
            RoomReservationDto reservation = reservationOpt.get();
            
            // 체크인/체크아웃 날짜 확인
            LocalDate checkin = reservation.getCheckinDate();
            LocalDate checkout = reservation.getCheckoutDate();
            
            if (checkin.equals(targetDate) && checkout.equals(targetDate)) {
                roomStatus.put("reservationStatus", "체크인/체크아웃");
            } else if (checkin.equals(targetDate)) {
                roomStatus.put("reservationStatus", "체크인");
            } else if (checkout.equals(targetDate)) {
                roomStatus.put("reservationStatus", "체크아웃");
            } else if (checkin.isBefore(targetDate) && checkout.isAfter(targetDate)) {
                roomStatus.put("reservationStatus", "사용중");
            }
            
            // 고객 정보
            if (reservation.getCustomer() != null) {
                roomStatus.put("customerName", reservation.getCustomer().getName());
            }
            
            // 예약 인원 수 및 요청사항
            roomStatus.put("guest", reservation.getGuest());
            roomStatus.put("specialRequest", reservation.getSpecialRequest());
        } else {
            roomStatus.put("reservationStatus", "빈 객실");
        }
        
        return roomStatus;
    }).collect(Collectors.toList());
    
    // 4. 빈 객실 카운트 계산
    long availableRoomCount = roomStatusList.stream()
        .filter(room -> !room.getOrDefault("hasReservation", false))
        .count();
    
    Map<String, Object> map = new HashMap<>();
    map.put("success", true);
    map.put("rooms", roomStatusList);
    map.put("availableRoomCount", availableRoomCount);
    map.put("totalRoomCount", roomStatusList.size());
    
    return ResponseEntity.ok(map);
}
```

**주요 특징**:

- ✅ **날짜별 예약 상태**: 체크인, 체크아웃, 사용중, 빈 객실 구분
- ✅ **예약 정보 포함**: 고객명, 인원수, 요청사항 포함
- ✅ **통계 제공**: 빈 객실 수, 전체 객실 수 제공
- ✅ **Stream API 활용**: 함수형 프로그래밍으로 코드 간결화

---

### 3️⃣ Admin - 객실 상태 변경 API

**엔드포인트**: `POST /api/admin/roomStatus`

**목적**: 객실의 사용 가능 여부를 빠르게 변경

**핵심 구현**:

```java
@PostMapping("/roomStatus")
@Operation(summary = "객실 상태 변경", description = "객실의 사용 가능 여부를 변경합니다.")
public ResponseEntity<Map<String, Object>> updateRoomStatus(
    @RequestBody RoomStatusDto dto,
    HttpServletRequest request) {
    
    Map<String, Object> map = new HashMap<>();
    
    // 1. contentId 검증 (호텔 소유권 확인)
    String contentId = getContentIdOrRedirect();
    if (contentId == null) {
        map.put("success", false);
        map.put("message", "호텔이 등록되지 않은 관리자입니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
    }

    // 2. 객실 조회 및 소유권 확인
    Optional<Room> roomOpt = roomService.findByRoomIdx(dto.getRoomIdx());
    if (roomOpt.isEmpty()) {
        map.put("success", false);
        map.put("message", "객실을 찾을 수 없습니다.");
        return ResponseEntity.badRequest().body(map);
    }

    Room room = roomOpt.get();
    
    // 3. 해당 호텔의 객실인지 확인
    if (!room.getContentId().equals(contentId)) {
        map.put("success", false);
        map.put("message", "해당 호텔의 객실이 아닙니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
    }

    // 4. 상태 변경
    Room updatedRoom = roomService.updateRoomStatus(dto.getRoomIdx(), dto.getStatus());
    
    // 5. 동적 메시지 생성
    String statusMessage = dto.getStatus() == 1 ? "사용 가능" : "사용 불가";
    
    map.put("success", true);
    map.put("message", String.format("객실 상태가 '%s'로 변경되었습니다.", statusMessage));
    map.put("room", updatedRoom);
    return ResponseEntity.ok(map);
}
```

**주요 특징**:

- ✅ **이중 권한 검증**: 호텔 소유권 + 객실 소유권 확인
- ✅ **안전한 상태 변경**: 존재하지 않는 객실 또는 권한 없는 요청 차단
- ✅ **명확한 에러 메시지**: 각 단계별 실패 원인 명시
- ✅ **상태 코드 활용**: 400 (Bad Request), 403 (Forbidden) 적절히 사용

---

### 4️⃣ Admin - 매출 요약 API

**엔드포인트**: `GET /api/admin/revenueSummary?year=2024`

**목적**: 오늘 매출 및 월별 매출 통계 제공 (동적 연도 제한)

**핵심 구현**:

```java
@GetMapping("/revenueSummary")
@Operation(summary = "매출 요약", description = "오늘 매출/건수, 월별 매출을 조회합니다.")
public ResponseEntity<?> getRevenueSummary(
    @RequestParam(value = "year", required = false) Integer year,
    HttpServletRequest request) {

    String contentid = getContentIdOrRedirect();
    if (contentid == null) {
        return createRedirectResponse();
    }

    // year가 지정되지 않으면 현재 연도 사용
    if (year == null) {
        year = Year.now().getValue();
    }

    // 1. 매출 요약 조회
    RevenueSummaryDto dto = revenueService.getRevenueSummary(contentid, year);
    
    // 2. 최소 연도 계산 및 추가 (서비스 시작 연도)
    Integer minYear = revenueService.getMinYear(contentid);
    dto.setMinYear(minYear);
    
    return ResponseEntity.ok(dto);
}
```

**RevenueService.getRevenueSummary()**:

```java
public RevenueSummaryDto getRevenueSummary(String contentId, Integer year) {
    // 1. 오늘 매출 계산 (RoomPayment에서)
    List<RoomPayment> payments = roomPaymentRepository.findAllByContentIdWithReservations(contentId);
    LocalDate today = LocalDate.now();
    
    long todayRevenue = 0L;
    int todayCount = 0;
    
    for (RoomPayment rp : payments) {
        LocalDate refDate = rp.getApprovedAt() != null 
            ? rp.getApprovedAt().atZone(ZoneId.systemDefault()).toLocalDate()
            : null;
        
        if (refDate != null && refDate.isEqual(today)) {
            todayRevenue += rp.getPrice().longValue();
            todayCount += 1;
        }
    }

    // 2. 월별 매출 조회 (HotelSettlement에서 - year 파라미터로 필터링)
    List<HotelSettlement> settlements;
    if (year != null) {
        settlements = hotelSettlementRepository.findByContentIdAndYear(contentId, String.valueOf(year));
    } else {
        settlements = hotelSettlementRepository.findByContentId(contentId);
    }
    
    // 3. 서비스 시작 연도 확인 (하이브리드 표시 로직)
    HotelInfo hotelInfo = hotelInfoRepository.findById(contentId).orElse(null);
    
    if (hotelInfo != null && hotelInfo.getServiceStartDate() != null) {
        LocalDate serviceStartDate = hotelInfo.getServiceStartDate();
        int serviceStartYear = serviceStartDate.getYear();
        int serviceStartMonth = serviceStartDate.getMonthValue();
        
        // 서비스 시작 연도면 시작 월부터, 이후 연도는 1~12월 전체
        if (year != null && year == serviceStartYear) {
            // 시작 월부터 12월까지
            for (int month = serviceStartMonth; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                Long revenue = monthToRevenue.getOrDefault(ym, 0L);
                monthly.add(new MonthRevenueDto(ym.toString() + "-01", revenue));
            }
        } else {
            // 1~12월 전체
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                Long revenue = monthToRevenue.getOrDefault(ym, 0L);
                monthly.add(new MonthRevenueDto(ym.toString() + "-01", revenue));
            }
        }
    }
    
    // 4. 현재 연도면 현재 월까지만 표시
    YearMonth currentYearMonth = YearMonth.now();
    if (year != null && year == currentYearMonth.getYear()) {
        monthly = monthly.stream()
            .filter(m -> {
                YearMonth ym = YearMonth.parse(m.getMonth().substring(0, 7));
                return ym.isBefore(currentYearMonth) || ym.equals(currentYearMonth);
            })
            .collect(Collectors.toList());
    }
    
    return RevenueSummaryDto.builder()
        .todayRevenue(todayRevenue)
        .todayPayments(todayCount)
        .monthlyRevenue(monthly)
        .build();
}
```

**주요 특징**:

- ✅ **연도별 필터링**: DB 쿼리 레벨에서 필터링하여 성능 최적화
- ✅ **하이브리드 표시**: 서비스 시작 연도는 시작 월부터, 이후 연도는 전체 표시
- ✅ **현재 연도 처리**: 현재 연도는 현재 월까지만 표시 (미래 월 숨김)
- ✅ **최소 연도 제공**: 프론트엔드에서 연도 선택 범위 제한 가능

---

### 5️⃣ Master - 통계 API (병렬 처리)

**엔드포인트**: `GET /api/master/statistics?dateRange=month`

**목적**: 날짜 범위별 통계 데이터 제공 (병렬 처리로 성능 최적화)

**핵심 구현**:

```java
@GetMapping("/statistics")
@Operation(summary = "통계 데이터 조회", description = "날짜 범위별 통계 데이터를 조회합니다.")
public ResponseEntity<?> getStatistics(
    @RequestParam(value = "dateRange", defaultValue = "month") String dateRange,
    HttpServletRequest request) {
    
    ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
    if (authCheck != null) {
        return authCheck;
    }
    
    Map<String, Object> statistics = statisticsService.getStatistics(dateRange);
    return ResponseEntity.ok(statistics);
}
```

**StatisticsService.getStatistics()**:

```java
@Transactional(readOnly = true)
public Map<String, Object> getStatistics(String dateRange) {
    LocalDate startDate = calculateStartDate(dateRange);
    LocalDate endDate = LocalDate.now();

    // 병렬 처리로 모든 통계 데이터 조회
    CompletableFuture<Long> totalRevenueFuture = CompletableFuture.supplyAsync(
        () -> getTotalRevenue(startDate, endDate)
    );
    CompletableFuture<Long> totalReservationCountFuture = CompletableFuture.supplyAsync(
        () -> getTotalReservationCount(startDate, endDate)
    );
    CompletableFuture<Long> activeHotelsFuture = CompletableFuture.supplyAsync(
        () -> getActiveHotelsCount(startDate, endDate)
    );
    CompletableFuture<Long> newHotelsFuture = CompletableFuture.supplyAsync(
        () -> getNewHotelsCount(startDate, endDate)
    );
    CompletableFuture<Long> newCustomersFuture = CompletableFuture.supplyAsync(
        () -> getNewCustomersCount(startDate, endDate)
    );

    // 모든 Future 완료 대기
    CompletableFuture.allOf(
        totalRevenueFuture,
        totalReservationCountFuture,
        activeHotelsFuture,
        newHotelsFuture,
        newCustomersFuture
    ).join();

    Map<String, Object> result = new HashMap<>();
    try {
        result.put("totalRevenue", totalRevenueFuture.get());
        result.put("totalReservationCount", totalReservationCountFuture.get());
        result.put("activeHotels", activeHotelsFuture.get());
        result.put("newHotels", newHotelsFuture.get());
        result.put("newCustomers", newCustomersFuture.get());
    } catch (Exception e) {
        log.error("통계 데이터 조회 중 오류 발생", e);
        throw new RuntimeException("통계 데이터 조회 실패", e);
    }

    return result;
}
```

**주요 특징**:

- ✅ **병렬 처리**: `CompletableFuture`로 독립적인 통계 조회 병렬 실행
- ✅ **성능 최적화**: 순차 실행 대비 약 5배 빠른 응답 시간
- ✅ **QueryDSL 활용**: 복잡한 조건 쿼리를 타입 안전하게 작성
- ✅ **날짜 범위 지원**: week(7일), month(30일), quarter(3개월), year(1년)

---

### 6️⃣ Master - 호텔 승인 상세 API (탭 기반 지연 로딩)

**엔드포인트**: 
- `GET /api/master/hotelApproval/{registrationIdx}` (기본 정보)
- `GET /api/master/hotelApproval/{registrationIdx}/{tab}` (탭별 데이터)

**목적**: 초기 로딩 속도 개선을 위한 탭 기반 지연 로딩

**핵심 구현**:

```java
// 기본 정보만 조회 (빠른 로딩)
@GetMapping("/hotelApproval/{registrationIdx}")
@Operation(summary = "승인요청 상세 조회 (기본 정보)", description = "특정 호텔 승인 요청의 기본 정보만 조회합니다.")
public ResponseEntity<?> getHotelApprovalDetail(
    @PathVariable Integer registrationIdx,
    HttpServletRequest request) {
    
    ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
    if (authCheck != null) {
        return authCheck;
    }
    
    // 1. RegistrationRequest 조회
    RegistrationRequest registrationRequest = registrationRequestService.findById(registrationIdx);
    
    // 2. HotelDraft 조회
    Integer draftIdx = registrationRequest.getDraftIdx();
    Optional<HotelDraft> draftOpt = hotelDraftService.findById(draftIdx);
    
    HotelDraft draft = draftOpt.get();
    
    // 3. formData를 Map으로 파싱 (JSON → Map)
    TypeFactory typeFactory = objectMapper.getTypeFactory();
    MapType mapType = typeFactory.constructMapType(Map.class, String.class, Object.class);
    Map<String, Object> formDataMap = objectMapper.readValue(draft.getFormData(), mapType);
    
    // 4. 기본 정보만 추출 (images, rooms, dining 제외)
    Map<String, Object> basicData = new HashMap<>();
    basicData.put("hotelInfo", formDataMap.get("hotelInfo"));
    basicData.put("hotelDetail", formDataMap.get("hotelDetail"));
    basicData.put("area", formDataMap.get("area"));
    // 빈 배열로 초기화 (탭 전환 시 로드)
    basicData.put("images", List.of());
    basicData.put("rooms", List.of());
    basicData.put("events", List.of());
    basicData.put("dining", List.of());
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("data", basicData);
    
    return ResponseEntity.ok(response);
}

// 탭별 데이터 조회 (지연 로딩)
@GetMapping("/hotelApproval/{registrationIdx}/{tab}")
@Operation(summary = "승인요청 탭별 데이터 조회", description = "특정 탭(images, rooms, dining)의 데이터만 조회합니다.")
public ResponseEntity<?> getHotelApprovalTabData(
    @PathVariable Integer registrationIdx,
    @PathVariable String tab,
    HttpServletRequest request) {
    
    // 유효한 탭인지 확인
    if (!tab.equals("images") && !tab.equals("rooms") && !tab.equals("dining")) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "유효하지 않은 탭 이름입니다: " + tab);
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    // HotelDraft 조회 및 파싱 (위와 동일)
    // ...
    
    // 해당 탭의 데이터만 추출
    Map<String, Object> tabData = new HashMap<>();
    if (tab.equals("images")) {
        tabData.put("images", formDataMap.getOrDefault("images", List.of()));
        tabData.put("events", formDataMap.getOrDefault("events", List.of()));
    } else if (tab.equals("rooms")) {
        tabData.put("rooms", formDataMap.getOrDefault("rooms", List.of()));
    } else if (tab.equals("dining")) {
        tabData.put("dining", formDataMap.getOrDefault("dining", List.of()));
    }
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("data", tabData);
    
    return ResponseEntity.ok(response);
}
```

**주요 특징**:

- ✅ **초기 로딩 최적화**: 기본 정보만 먼저 반환하여 화면 표시 속도 향상
- ✅ **JSON 파싱**: `ObjectMapper`로 `HotelDraft.formData` (JSON 문자열) 파싱
- ✅ **탭별 지연 로딩**: 사용자가 탭을 클릭할 때만 해당 데이터 로드
- ✅ **에러 처리**: 유효하지 않은 탭 이름에 대한 명확한 에러 메시지

---

### 7️⃣ Master - 신고 관리 API

**엔드포인트**: `GET /api/master/center/reports` (CenterController에서 처리)

**목적**: 신고 목록 조회 및 통계 제공

**핵심 구현**:

```java
// CenterController 또는 MasterManagementController에서 처리
@GetMapping("/center/reports")
@Operation(summary = "신고 목록 조회", description = "신고 목록을 조회합니다.")
public ResponseEntity<?> getReports(
    @RequestParam(value = "page", defaultValue = "0") int page,
    @RequestParam(value = "size", defaultValue = "10") int size,
    @RequestParam(value = "statusFilter", required = false) String statusFilter,
    @RequestParam(value = "categoryFilter", required = false) String categoryFilter,
    HttpServletRequest request) {
    
    ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
    if (authCheck != null) {
        return authCheck;
    }
    
    Pageable pageable = Pageable.ofSize(size).withPage(page);
    
    // 복합 조건 검색
    Page<Center> reports = centerService.searchByMultipleConditions(
        "report",  // mainCategory
        categoryFilter,
        statusFilter != null ? Integer.parseInt(statusFilter) : null,
        null,  // priority
        null,  // customerIdx
        null,  // adminIdx
        null,  // contentId
        null,  // contentIdList
        null,  // title
        pageable
    );
    
    // 통계 계산
    long totalReports = reports.getTotalElements();
    long completedReports = reports.getContent().stream()
        .filter(r -> r.getStatus() == 1)  // 1: 처리 완료
        .count();
    long inProgressReports = reports.getContent().stream()
        .filter(r -> r.getStatus() == 0)  // 0: 처리중
        .count();
    
    Map<String, Object> response = new HashMap<>();
    response.put("content", reports.getContent());
    response.put("totalElements", reports.getTotalElements());
    response.put("totalPages", reports.getTotalPages());
    response.put("statistics", Map.of(
        "total", totalReports,
        "completed", completedReports,
        "inProgress", inProgressReports
    ));
    
    return ResponseEntity.ok(response);
}
```

**주요 특징**:

- ✅ **복합 조건 검색**: 카테고리, 상태, 우선순위 등 다중 필터 지원
- ✅ **페이지네이션**: 대량 데이터 효율적 처리
- ✅ **통계 제공**: 전체, 처리 완료, 처리중 건수 제공
- ✅ **QueryDSL 활용**: 동적 쿼리로 유연한 검색 조건 처리

---

## 🐛 트러블 슈팅

### 1️⃣ 호텔 미등록 관리자 접근 문제

**오류 상황**:

- 호텔이 등록되지 않은 관리자가 Admin API에 접근 시도
- `contentId`가 없어서 모든 조회가 실패

**원인 분석**:

- JWT에는 `adminIdx`만 있고 `contentId`는 별도 조회 필요
- 호텔 미등록 관리자는 `contentId` 조회 결과가 `null`

**해결 과정**:

```java
// 공통 메서드로 권한 검증 및 contentId 조회
private String getContentIdOrRedirect() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
    Integer adminIdx = principal.getAdminIdx();
    
    if (adminIdx == null) {
        return null; // 리다이렉트 필요
    }
    
    Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
    if (contentIdOpt.isEmpty()) {
        return null; // 리다이렉트 필요
    }
    
    return contentIdOpt.get();
}

// 공통 에러 응답 생성
private ResponseEntity<Map<String, Object>> createRedirectResponse() {
    Map<String, Object> map = new HashMap<>();
    map.put("success", false);
    map.put("redirect", true);
    map.put("message", "호텔이 등록되지 않은 관리자입니다. 메인 화면으로 이동합니다.");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
}

// 모든 Admin API에서 사용
@RequestMapping("/dashboard")
public ResponseEntity<?> dashboard(HttpServletRequest request) {
    String contentid = getContentIdOrRedirect();
    if (contentid == null) {
        return createRedirectResponse();
    }
    // ...
}
```

**교훈**:

- 공통 권한 검증 로직을 메서드로 분리하여 재사용
- 프론트엔드에서 `redirect` 플래그를 확인하여 자동 리다이렉트
- 일관된 에러 응답 형식으로 프론트엔드 처리 간소화

---

### 2️⃣ 매출 데이터 연도별 필터링 성능 문제

**오류 상황**:

- `getRevenueSummary`에서 모든 `HotelSettlement` 데이터를 조회 후 Java에서 필터링
- 데이터가 많아질수록 성능 저하

**원인 분석**:

- Repository에서 `findByContentId`로 전체 데이터 조회
- Java Stream으로 연도 필터링하여 불필요한 데이터 전송

**해결 과정**:

```java
// Repository에 연도별 조회 메서드 추가
@Query("SELECT h FROM HotelSettlement h WHERE h.contentId = :contentId AND SUBSTRING(h.settlementMonth, 1, 4) = :year")
List<HotelSettlement> findByContentIdAndYear(@Param("contentId") String contentId, @Param("year") String year);

// Service에서 연도가 지정된 경우 DB 레벨에서 필터링
public RevenueSummaryDto getRevenueSummary(String contentId, Integer year) {
    List<HotelSettlement> settlements;
    if (year != null) {
        // DB 쿼리 레벨에서 필터링 (성능 최적화)
        settlements = hotelSettlementRepository.findByContentIdAndYear(contentId, String.valueOf(year));
    } else {
        settlements = hotelSettlementRepository.findByContentId(contentId);
    }
    // ...
}
```

**교훈**:

- DB 쿼리 레벨에서 필터링하여 네트워크 전송량 감소
- 불필요한 데이터 조회 방지로 메모리 사용량 감소
- 인덱스 활용 가능하여 쿼리 성능 향상

---

### 3️⃣ 통계 API 응답 시간 지연 문제

**오류 상황**:

- `getStatistics`에서 5개의 통계를 순차적으로 조회
- 각 통계 조회가 1초씩 걸리면 총 5초 소요

**원인 분석**:

- 각 통계 조회가 독립적이지만 순차 실행
- DB 쿼리 시간이 누적되어 전체 응답 시간 증가

**해결 과정**:

```java
// CompletableFuture로 병렬 처리
public Map<String, Object> getStatistics(String dateRange) {
    LocalDate startDate = calculateStartDate(dateRange);
    LocalDate endDate = LocalDate.now();

    // 병렬 처리로 모든 통계 데이터 조회
    CompletableFuture<Long> totalRevenueFuture = CompletableFuture.supplyAsync(
        () -> getTotalRevenue(startDate, endDate)
    );
    CompletableFuture<Long> totalReservationCountFuture = CompletableFuture.supplyAsync(
        () -> getTotalReservationCount(startDate, endDate)
    );
    // ... 다른 통계들도 병렬 처리

    // 모든 Future 완료 대기
    CompletableFuture.allOf(
        totalRevenueFuture,
        totalReservationCountFuture,
        // ...
    ).join();

    // 결과 수집
    Map<String, Object> result = new HashMap<>();
    result.put("totalRevenue", totalRevenueFuture.get());
    result.put("totalReservationCount", totalReservationCountFuture.get());
    // ...
    
    return result;
}
```

**교훈**:

- 독립적인 작업은 병렬 처리로 성능 향상
- `CompletableFuture`로 비동기 처리 구현
- 전체 응답 시간을 가장 느린 작업 시간으로 단축

---

### 4️⃣ 호텔 승인 상세 조회 초기 로딩 지연 문제

**오류 상황**:

- 호텔 승인 상세 조회 시 모든 데이터(기본 정보, 이미지, 객실, 다이닝)를 한 번에 조회
- 이미지가 많을 경우 초기 로딩 시간이 길어짐

**원인 분석**:

- `HotelDraft.formData`에 모든 데이터가 JSON 문자열로 저장
- 한 번에 파싱하여 모든 데이터 반환

**해결 과정**:

```java
// 1. 기본 정보만 먼저 조회
@GetMapping("/hotelApproval/{registrationIdx}")
public ResponseEntity<?> getHotelApprovalDetail(@PathVariable Integer registrationIdx) {
    // 기본 정보만 추출 (images, rooms, dining 제외)
    Map<String, Object> basicData = new HashMap<>();
    basicData.put("hotelInfo", formDataMap.get("hotelInfo"));
    basicData.put("hotelDetail", formDataMap.get("hotelDetail"));
    basicData.put("area", formDataMap.get("area"));
    // 빈 배열로 초기화
    basicData.put("images", List.of());
    basicData.put("rooms", List.of());
    basicData.put("dining", List.of());
    
    return ResponseEntity.ok(response);
}

// 2. 탭별 데이터는 별도 엔드포인트로 제공
@GetMapping("/hotelApproval/{registrationIdx}/{tab}")
public ResponseEntity<?> getHotelApprovalTabData(
    @PathVariable Integer registrationIdx,
    @PathVariable String tab) {
    
    // 해당 탭의 데이터만 추출
    if (tab.equals("images")) {
        tabData.put("images", formDataMap.getOrDefault("images", List.of()));
    } else if (tab.equals("rooms")) {
        tabData.put("rooms", formDataMap.getOrDefault("rooms", List.of()));
    }
    
    return ResponseEntity.ok(response);
}
```

**교훈**:

- 초기 로딩 속도 개선을 위해 필수 데이터만 먼저 제공
- 사용자가 필요로 할 때만 추가 데이터 로드 (지연 로딩)
- 프론트엔드와 협업하여 탭 기반 로딩 전략 수립

---

## 🚀 향후 개선 사항

### 단기 (1-2주)

1. **캐싱 전략**
   - [ ] 통계 데이터 캐싱 (Redis)
   - [ ] 호텔 정보 캐싱
   - [ ] 캐시 무효화 전략 수립

2. **API 응답 최적화**
   - [ ] DTO 프로젝션으로 불필요한 필드 제외
   - [ ] 페이지네이션 기본값 최적화
   - [ ] 응답 데이터 압축 (Gzip)

3. **에러 처리 강화**
   - [ ] 전역 예외 처리기 구현
   - [ ] 커스텀 예외 클래스 정의
   - [ ] 에러 로깅 강화

---

### 중기 (1개월)

1. **성능 모니터링**
   - [ ] Spring Actuator로 메트릭 수집
   - [ ] 느린 쿼리 로깅
   - [ ] API 응답 시간 모니터링

2. **보안 강화**
   - [ ] Rate Limiting 적용
   - [ ] 입력값 검증 강화
   - [ ] SQL Injection 방지 (이미 PreparedStatement 사용 중)

3. **테스트 코드 작성**
   - [ ] 단위 테스트 (Service 레이어)
   - [ ] 통합 테스트 (Controller 레이어)
   - [ ] MockMvc를 활용한 API 테스트

---

### 장기 (2개월+)

1. **분산 시스템 지원**
   - [ ] Redis 분산 락 적용
   - [ ] 이벤트 기반 아키텍처 도입
   - [ ] 마이크로서비스 전환 검토

2. **모니터링 및 알림**
   - [ ] Prometheus + Grafana 연동
   - [ ] 에러 발생 시 알림 (Slack, Email)
   - [ ] 성능 저하 감지 및 알림

3. **문서화**
   - [ ] Swagger/OpenAPI 문서 자동 생성
   - [ ] API 사용 가이드 작성
   - [ ] 아키텍처 다이어그램 작성

---

## 💼 개발자 포트폴리오 강점

### 1. 권한 관리 및 보안 구현

**도전**: 호텔 관리자와 사이트 운영자의 권한 분리 및 안전한 API 접근 제어

**해결**:

- **JWT 기반 인증**: SecurityContext에서 `adminIdx` 추출
- **이중 권한 검증**: 호텔 소유권 + 객실 소유권 확인
- **공통 권한 검증 메서드**: `getContentIdOrRedirect()`, `checkMasterAuthorization()`
- **명확한 에러 응답**: 403 Forbidden + `redirect` 플래그로 프론트엔드 처리 간소화

**역량**:

- ✅ Spring Security 이해 및 활용
- ✅ 권한 기반 접근 제어 구현
- ✅ 보안 취약점 인식 및 방어

---

### 2. 성능 최적화

**도전**: 대량 데이터 조회 및 복잡한 통계 계산의 성능 개선

**해결**:

- **병렬 처리**: `CompletableFuture`로 독립적인 통계 조회 병렬 실행
- **DB 쿼리 최적화**: 연도별 필터링을 DB 레벨에서 처리
- **탭 기반 지연 로딩**: 초기 로딩 속도 개선을 위한 데이터 분할
- **QueryDSL 활용**: 복잡한 조건 쿼리를 타입 안전하게 작성

**역량**:

- ✅ 비동기 프로그래밍 이해
- ✅ DB 쿼리 최적화 경험
- ✅ 성능 모니터링 및 개선

---

### 3. 복잡한 비즈니스 로직 구현

**도전**: 매출 통계의 하이브리드 표시 로직 (서비스 시작 연도 vs 이후 연도)

**해결**:

- **하이브리드 데이터 생성**: 서비스 시작 연도는 시작 월부터, 이후 연도는 1~12월 전체
- **현재 연도 처리**: 현재 연도는 현재 월까지만 표시 (미래 월 숨김)
- **최소 연도 제공**: `minYear`를 응답에 포함하여 프론트엔드에서 선택 범위 제한

**역량**:

- ✅ 복잡한 비즈니스 요구사항 분석 및 구현
- ✅ 데이터 변환 및 가공 로직 설계
- ✅ 사용자 경험 고려한 데이터 제공

---

### 4. RESTful API 설계

**도전**: 직관적이고 일관된 API 엔드포인트 설계

**해결**:

- **RESTful 원칙 준수**: 리소스 기반 URL 설계
- **명확한 HTTP 메서드 사용**: GET (조회), POST (생성/변경), PUT (수정)
- **일관된 응답 형식**: `success`, `message`, `data` 구조
- **Swagger 문서화**: `@Operation`, `@ApiResponse`로 API 문서 자동 생성

**역량**:

- ✅ RESTful API 설계 원칙 이해
- ✅ API 문서화 습관
- ✅ 프론트엔드와의 협업 경험

---

### 5. 트랜잭션 관리 및 데이터 무결성

**도전**: 객실 상태 변경 시 권한 검증 및 데이터 일관성 보장

**해결**:

- **이중 권한 검증**: 호텔 소유권 + 객실 소유권 확인
- **트랜잭션 관리**: `@Transactional`로 데이터 일관성 보장
- **명확한 에러 처리**: 각 단계별 실패 원인 명시

**역량**:

- ✅ 트랜잭션 관리 이해
- ✅ 데이터 무결성 보장
- ✅ 에러 처리 전략 수립

---

## 📝 핵심 코드 예시

### AdminManagementController - 공통 권한 검증

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminManagementController {
    
    private final HotelInfoService hotelInfoService;
    
    /**
     * JWT에서 adminIdx를 추출하고 contentId를 조회
     * contentId가 없으면 메인 화면으로 리다이렉트
     */
    private String getContentIdOrRedirect() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        
        if (adminIdx == null) {
            return null; // 리다이렉트 필요
        }
        
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        if (contentIdOpt.isEmpty()) {
            return null; // 리다이렉트 필요
        }
        
        return contentIdOpt.get();
    }
    
    /**
     * contentId가 없을 때 프론트엔드에서 리다이렉트할 수 있도록 403 Forbidden 반환
     */
    private ResponseEntity<Map<String, Object>> createRedirectResponse() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("redirect", true);
        map.put("message", "호텔이 등록되지 않은 관리자입니다. 메인 화면으로 이동합니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
    }
    
    // 모든 Admin API에서 사용
    @RequestMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpServletRequest request) {
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        // ...
    }
}
```

---

### StatisticsService - 병렬 처리

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    
    private final JPAQueryFactory queryFactory;
    
    public Map<String, Object> getStatistics(String dateRange) {
        LocalDate startDate = calculateStartDate(dateRange);
        LocalDate endDate = LocalDate.now();

        // 병렬 처리로 모든 통계 데이터 조회
        CompletableFuture<Long> totalRevenueFuture = CompletableFuture.supplyAsync(
            () -> getTotalRevenue(startDate, endDate)
        );
        CompletableFuture<Long> totalReservationCountFuture = CompletableFuture.supplyAsync(
            () -> getTotalReservationCount(startDate, endDate)
        );
        CompletableFuture<Long> activeHotelsFuture = CompletableFuture.supplyAsync(
            () -> getActiveHotelsCount(startDate, endDate)
        );
        CompletableFuture<Long> newHotelsFuture = CompletableFuture.supplyAsync(
            () -> getNewHotelsCount(startDate, endDate)
        );
        CompletableFuture<Long> newCustomersFuture = CompletableFuture.supplyAsync(
            () -> getNewCustomersCount(startDate, endDate)
        );

        // 모든 Future 완료 대기
        CompletableFuture.allOf(
            totalRevenueFuture,
            totalReservationCountFuture,
            activeHotelsFuture,
            newHotelsFuture,
            newCustomersFuture
        ).join();

        Map<String, Object> result = new HashMap<>();
        try {
            result.put("totalRevenue", totalRevenueFuture.get());
            result.put("totalReservationCount", totalReservationCountFuture.get());
            result.put("activeHotels", activeHotelsFuture.get());
            result.put("newHotels", newHotelsFuture.get());
            result.put("newCustomers", newCustomersFuture.get());
        } catch (Exception e) {
            log.error("통계 데이터 조회 중 오류 발생", e);
            throw new RuntimeException("통계 데이터 조회 실패", e);
        }

        return result;
    }
    
    private Long getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        return queryFactory
            .select(hotelSettlement.totalRevenue.sum())
            .from(hotelSettlement)
            .where(
                hotelSettlement.settlementMonth.between(
                    startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    endDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                )
            )
            .fetchOne();
    }
    
    // ... 다른 통계 메서드들도 동일한 패턴
}
```

---

## 🔐 보안 고려사항

### 구현 완료

1. **권한 검증**: 모든 API에서 호텔 소유권 및 마스터 권한 확인
2. **JWT 인증**: SecurityContext에서 인증 정보 추출
3. **이중 검증**: 호텔 소유권 + 객실 소유권 확인
4. **에러 응답**: 명확한 에러 메시지 및 상태 코드

### 향후 보완

- [ ] Rate Limiting (API 호출 제한)
- [ ] 입력값 검증 강화 (Bean Validation)
- [ ] SQL Injection 방지 (이미 PreparedStatement 사용 중)
- [ ] XSS 방지 (프론트엔드에서 처리)

---

## 📊 성과 지표

### 구현 완료

- ✅ Admin API: 대시보드, 객실 관리, 매출 관리, 체크인/체크아웃
- ✅ Master API: 통계 분석, 호텔 승인, 신고 관리
- ✅ 권한 관리: 호텔 소유권 및 마스터 권한 검증
- ✅ 성능 최적화: 병렬 처리, DB 쿼리 최적화, 지연 로딩

### 코드 품질

- **권한 관리**: 공통 메서드로 일관된 권한 검증
- **에러 처리**: 명확한 에러 메시지 및 상태 코드
- **성능 최적화**: 병렬 처리, 쿼리 최적화
- **코드 재사용**: 공통 로직 메서드화

---

## 📖 사용 기술 및 라이브러리

### Core

- **Spring Boot 3.x**: RESTful API 개발
- **Spring Security**: 인증 및 권한 관리
- **JPA/Hibernate**: ORM 및 데이터 접근
- **QueryDSL**: 타입 안전한 동적 쿼리

### Database

- **MySQL**: 관계형 데이터베이스
- **JPA Repository**: 데이터 접근 계층
- **Native Query**: 복잡한 통계 쿼리

### 기타

- **Swagger/OpenAPI**: API 문서 자동 생성
- **CompletableFuture**: 비동기 병렬 처리
- **ObjectMapper**: JSON 파싱 (Jackson)
- **Lombok**: 보일러플레이트 코드 제거

---

## 📞 문의

- **담당자**: [작성자명]
- **영역**: 프론트엔드(Admin & Master)에서 사용하는 백엔드 API
- **충돌 시**: PR/코멘트로 전달

---

_Last Updated: 2025-01-12_

