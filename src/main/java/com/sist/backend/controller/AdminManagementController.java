package com.sist.backend.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.Customer;
import com.sist.backend.service.CouponService;
import com.sist.backend.service.CouponTemplateService;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.ReservationTimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.sist.backend.dto.admin.CheckTimeUpdateDto;
import com.sist.backend.dto.admin.CouponCreateDto;
import com.sist.backend.dto.admin.RoomReservationDto;
import com.sist.backend.dto.admin.RoomStatusDto;
import com.sist.backend.dto.admin.RoomUpdateDto;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Room;
import com.sist.backend.service.RoomPaymentService;
import com.sist.backend.service.RevenueService;
import com.sist.backend.service.RoomReservationService;
import com.sist.backend.service.RoomService;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.dto.admin.RevenueSummaryDto;
import com.sist.backend.dto.admin.CustomerStatsDto;
import com.sist.backend.dto.admin.CustomerListDto;
import com.sist.backend.dto.admin.CustomerHistoryDto;
import com.sist.backend.dto.admin.CustomerHistoryStatsDto;
import com.sist.backend.dto.admin.FeedbackDto;
import com.sist.backend.dto.admin.FeedbackStatsDto;
import com.sist.backend.entity.ReviewAnswer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminManagementController {

    private final RoomReservationService roomReservationService;
    private final RoomPaymentService roomPaymentService;
    private final RevenueService revenueService;
    private final ReservationTimeService reservationTimeService;
    private final RoomService roomService;
    private final CouponTemplateService couponTemplateService;
    private final CustomerService customerService;
    private final CouponService couponService;
    private final HotelInfoService hotelInfoService;
    private final com.sist.backend.service.ReviewService reviewService;

    /**
     * JWT에서 adminIdx를 추출하고 contentId를 조회
     * contentId가 없으면 메인 화면으로 리다이렉트
     * @return contentId 문자열, 리다이렉트가 필요한 경우 null (이 경우 즉시 리다이렉트 응답 반환 필요)
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
            // contentId가 없으면 호텔이 등록되지 않은 관리자이므로 메인 화면으로 리다이렉트
            return null; // 리다이렉트 필요
        }
        
        return contentIdOpt.get();
    }

    /**
     * contentId가 없을 때 프론트엔드에서 리다이렉트할 수 있도록 403 Forbidden 반환
     * 모든 엔드포인트에서 사용할 수 있는 공통 에러 응답
     */
    private ResponseEntity<Map<String, Object>> createRedirectResponse() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("redirect", true);
        map.put("message", "호텔이 등록되지 않은 관리자입니다. 메인 화면으로 이동합니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
    }

    /* 호텔 관리자 대시보드 화면 */
    /* 오늘 체크인, 오늘 체크아웃(roomReservation), 예약 대기, 이번달 매출 */
    /* 최근 예약 현황 */
    @RequestMapping("/dashboard")
    @Operation(summary = "대시보드 관리자", description = "관리자 대시보드 화면")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> dashboard(
        @Parameter(description = "HTTP 요청", hidden = true) 
        HttpServletRequest request){
        // adminIdx로 contentId 조회
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 오늘 체크인한 사람의 수
        Integer todayCheckinCount = roomReservationService.getTodayCheckinCount();
        // 오늘 체크아웃한 사람의 수
        Integer todayCheckoutCount = roomReservationService.getTodayCheckoutCount();
        // 예약 확정 사람 수
        Integer reservationCount = roomReservationService.findByStatus();
        // 이번달 매출
        Long thisMonthSales = roomPaymentService.findByPrice();

        /* 최근 예약 현황 조회 (5개만) - Room과 Customer 정보 포함 */
        List<RoomReservationDto> roomReservationList = roomReservationService.findByStatusWithDetails(contentid);
        com.sist.backend.dto.admin.DashboardDto.Today today = com.sist.backend.dto.admin.DashboardDto.Today.builder()
            .checkinCount(todayCheckinCount != null ? todayCheckinCount : 0)
            .checkoutCount(todayCheckoutCount != null ? todayCheckoutCount : 0)
            .reservationCount(reservationCount != null ? reservationCount : 0)
            .thisMonthSales(thisMonthSales != null ? thisMonthSales : 0)
            .build();

        com.sist.backend.dto.admin.DashboardDto dto = com.sist.backend.dto.admin.DashboardDto.builder()
            .today(today)
            .recentReservations(roomReservationList)
            .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/revenueSummary")
    @Operation(summary = "매출 요약", description = "오늘 매출/건수, 월별 매출, 객실명별 매출을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getRevenueSummary(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {

        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }

        RevenueSummaryDto dto = revenueService.getRevenueSummary(contentid);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/revenueDaily")
    @Operation(summary = "일별 매출", description = "지정 구간의 일별 매출/건수를 조회합니다. start,end는 YYYY-MM-DD")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getDailyRevenue(
        @Parameter(description = "시작일(YYYY-MM-DD)") @RequestParam("start") String start,
        @Parameter(description = "종료일(YYYY-MM-DD)") @RequestParam("end") String end,
        @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }

        java.time.LocalDate s;
        java.time.LocalDate e;
        try {
            s = java.time.LocalDate.parse(start);
            e = java.time.LocalDate.parse(end);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
        if (e.isBefore(s)) {
            return ResponseEntity.badRequest().build();
        }

        List<com.sist.backend.dto.admin.DailyRevenueDto> rows = revenueService.getDailyRevenue(contentid, s, e);
        return ResponseEntity.ok(rows);
    }

    @RequestMapping("/roomReservationList")
    @Operation(summary = "예약 목록 조회", description = "호텔의 모든 예약 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> recentReservations(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "10") 
        @RequestParam(value = "size", defaultValue = "10") int size,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request){
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(roomReservationService.findByStatusDto(contentid, pageable));
    }

    @RequestMapping("/checkinPendingList")
    @Operation(summary = "체크인 현황", description = "체크인 현황을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findContentIdByAdminIdx(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "5") 
        @RequestParam(value = "size", defaultValue = "5") int size,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request){
        Pageable pageable = Pageable.ofSize(size).withPage(page);

        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }

        return ResponseEntity.ok(roomPaymentService.findByOrderIdxAndInTime(contentid, pageable));
    }

    @PostMapping("/checkin")
    @Operation(summary = "체크인 처리", description = "체크인을 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 처리됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> checkin(
        @RequestBody CheckTimeUpdateDto dto,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest httpRequest){
        Map<String, Object> map = new HashMap<>();
        
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        try {
            // DTO에 현재 시간 설정 (프론트엔드에서 전달하지 않은 경우를 대비)
            if (dto.getInTime() == null) {
                dto.setInTime(java.time.LocalDateTime.now());
            }
            
            // DTO를 사용하여 저장 (없으면 생성, 있으면 업데이트)
            reservationTimeService.checkin(dto);
            
            map.put("success", true);
            map.put("message", "체크인 처리가 완료되었습니다.");
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "체크인 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }

        return ResponseEntity.ok(map);
    }

    @RequestMapping("/checkoutPendingList")
    @Operation(summary = "체크아웃 현황 현황", description = "체크아웃 현황을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findByOrderIdxAndOutTime(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "5") 
        @RequestParam(value = "size", defaultValue = "5") int size,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request){
        Pageable pageable = Pageable.ofSize(size).withPage(page);

        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }

        return ResponseEntity.ok(roomPaymentService.findByOrderIdxAndOutTime(contentid, pageable));
    }

    @PostMapping("/checkout")
    @Operation(summary = "체크아웃 처리", description = "체크아웃을 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 처리됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> checkout(
        @RequestBody CheckTimeUpdateDto dto,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest httpRequest){
        Map<String, Object> map = new HashMap<>();
        
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        try {
            // DTO에 현재 시간 설정 (프론트엔드에서 전달하지 않은 경우를 대비)
            if (dto.getOutTime() == null) {
                dto.setOutTime(java.time.LocalDateTime.now());
            }
            
            // DTO를 사용하여 저장 (없으면 생성, 있으면 업데이트)
            reservationTimeService.checkout(dto);
            
            map.put("success", true);
            map.put("message", "체크아웃 처리가 완료되었습니다.");
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "체크아웃 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }

        return ResponseEntity.ok(map);
    }

    @RequestMapping("/couponIssue")
    @Operation(summary = "쿠폰 발급 화면", description = "쿠폰 발급 화면을 보여줍니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getCouponIssuePage(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size,
            @Parameter(description = "HTTP 요청", hidden = true)
            HttpServletRequest request){
        
        Map<String, Object> map = new HashMap<>();
        
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        // Pageable 생성
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        
        map.put("couponTemplates", couponTemplateService.findByStatus());
        map.put("coupons", couponService.findByAdminIdx(adminIdx, pageable));
        
        return ResponseEntity.ok(map);
    }
    
    @GetMapping("/roomList")
    @Operation(summary = "객실 현황 조회", description = "특정 날짜 기준 객실 현황을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getRoomStatus(
        @Parameter(description = "조회 날짜", example = "2024-01-01")
        @RequestParam(value = "date", required = false) String dateStr,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 날짜 처리 (없으면 오늘 날짜)
        LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) 
            ? LocalDate.parse(dateStr) 
            : LocalDate.now();
        
        // 객실 리스트 조회
        List<Room> rooms = roomService.findByContentIdAdmin(contentid);
        
        // 해당 날짜 예약 조회
        List<RoomReservationDto> dateReservations = roomReservationService.findByDateRangeWithDetails(contentid, targetDate, targetDate);
        
        // 각 객실의 상태 설정
        List<Map<String, Object>> roomStatusList = rooms.stream().map(room -> {
            Map<String, Object> roomStatus = new HashMap<>();
            
            // 객실 기본 정보
            roomStatus.put("roomIdx", room.getRoomIdx());
            roomStatus.put("contentId", room.getContentId());
            roomStatus.put("name", room.getName());
            roomStatus.put("capacity", room.getCapacity());
            roomStatus.put("basePrice", room.getBasePrice());
            roomStatus.put("status", room.getStatus()); // 객실 기본 상태
            
            // 해당 날짜 이 객실의 예약 찾기
            Optional<RoomReservationDto> reservationOpt = dateReservations.stream()
                .filter(reservation -> reservation.getRoomIdx().equals(room.getRoomIdx()))
                .findFirst();
            
            if (reservationOpt.isPresent()) {
                RoomReservationDto reservation = reservationOpt.get();
                roomStatus.put("reservationStatus", "예약");
                roomStatus.put("hasReservation", true);
                
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
            } else {
                roomStatus.put("reservationStatus", "빈 객실");
                roomStatus.put("hasReservation", false);
            }
            
            return roomStatus;
        }).collect(Collectors.toList());
        
        // 빈 객실 카운트 계산
        long availableRoomCount = roomStatusList.stream()
            .filter(room -> !((Boolean) room.getOrDefault("hasReservation", false)))
            .count();
        
        map.put("success", true);
        map.put("rooms", roomStatusList);
        map.put("targetDate", targetDate.toString());
        map.put("availableRoomCount", availableRoomCount);
        map.put("totalRoomCount", roomStatusList.size());
        
        return ResponseEntity.ok(map);
    }

    @GetMapping("/calendar")
    @Operation(summary = "예약 달력 조회", description = "달력 형식으로 예약을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getCalendarReservations(
        @Parameter(description = "조회 시작 날짜", example = "2024-01-01")
        @RequestParam(value = "startDate", required = false) String startDate,
        @Parameter(description = "조회 종료 날짜", example = "2024-01-31")
        @RequestParam(value = "endDate", required = false) String endDate,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 날짜가 없으면 이번 달 1일부터 한 달간
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : start.plusMonths(1).minusDays(1);
        
        List<RoomReservationDto> reservations = roomReservationService.findByDateRangeWithDetails(contentid, start, end);
        
        map.put("success", true);
        map.put("reservations", reservations);
        map.put("startDate", start);
        map.put("endDate", end);
        
        return ResponseEntity.ok(map);
    }

    @RequestMapping("/recentCustomers")
    @Operation(summary = "최근 이용 고객 조회", description = "해당 호텔을 최근에 이용한 고객 5명을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getRecentCustomers(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 최근 예약 고객 조회
        List<RoomReservationDto> recentReservations = roomReservationService.findByStatusWithDetails(contentid);
        
        map.put("success", true);
        map.put("customers", recentReservations);
        
        return ResponseEntity.ok(map);
    }

    @RequestMapping("/hotelCustomers")
    @Operation(summary = "호텔 사용 고객 검색", description = "해당 호텔을 이용한 고객 중에서 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> searchCustomers(
        @Parameter(description = "검색어", example = "jiwo")
        @RequestParam(value = "searchTerm", defaultValue = "") String searchTerm,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request){
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        if (searchTerm.trim().isEmpty()) {
            // 검색어가 없으면 빈 리스트 반환
            map.put("success", true);
            map.put("customers", List.of());
        } else {
            // 해당 호텔을 이용한 고객 중에서 검색
            List<Customer> customers = customerService.findByContentIdAndSearchTerm(contentid, searchTerm);
            map.put("success", true);
            map.put("customers", customers);
        }
        
        return ResponseEntity.ok(map);
    }

    @PostMapping("/couponCreate")
    @Operation(summary = "쿠폰 생성", description = "관리자가 고객에게 쿠폰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰이 성공적으로 생성됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> createCoupon(
        @RequestBody CouponCreateDto dto,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {

        Map<String, Object> map = new HashMap<>();
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }

        Coupon coupon = couponService.createCoupon(dto.getTemplateIdx(), dto.getCustomerIdx(), adminIdx);
        map.put("success", true);
        map.put("message", "쿠폰이 성공적으로 생성되었습니다.");
        map.put("coupon", coupon);

        return ResponseEntity.ok(map);
    }

    @GetMapping("/hotel/{adminIdx}")
    @Operation(summary = "관리자별 호텔 정보 조회", description = "adminIdx로 해당 관리자의 호텔 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 관리자의 호텔 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<String> findContentIdByAdminIdx(
        @Parameter(description = "관리자 ID", example = "1")
        @PathVariable("adminIdx") Integer adminIdx) {
        
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        
        if (contentIdOpt.isPresent()) {
            return ResponseEntity.ok(contentIdOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/roomUpdate")
    @Operation(summary = "객실 정보 수정", description = "객실 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 수정됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateRoom(
        @RequestBody RoomUpdateDto dto,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }

        Room room = roomService.updateRoom(dto.getRoomIdx(), dto.getName(), dto.getCapacity(), dto.getBasePrice());
        map.put("success", true);
        map.put("message", "객실 정보가 성공적으로 수정되었습니다.");
        map.put("room", room);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/roomStatus")
    @Operation(summary = "객실 비활성화", description = "객실을 비활성화 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 비활성화됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateRoomStatus(
        @RequestBody RoomStatusDto dto,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }

        Room room = roomService.updateRoomStatus(dto.getRoomIdx(), dto.getStatus());
        map.put("success", true);
        map.put("message", "객실 비활성화 처리가 완료되었습니다.");
        map.put("room", room);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/customerStats")
    @Operation(summary = "고객 통계 조회", description = "특정 호텔의 고객 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getCustomerStats(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 통계 계산
        Long totalCustomers = roomReservationService.countDistinctCustomersByContentId(contentid);
        Long newCustomersThisMonth = roomReservationService.countNewCustomersThisMonth(contentid);
        Double averagePaymentAmount = roomPaymentService.findAveragePaymentByContentId(contentid);
        
        CustomerStatsDto stats = new CustomerStatsDto();
        stats.setTotalCustomers(totalCustomers != null ? totalCustomers : 0L);
        stats.setNewCustomersThisMonth(newCustomersThisMonth != null ? newCustomersThisMonth : 0L);
        stats.setAveragePaymentAmount(averagePaymentAmount != null ? averagePaymentAmount : 0.0);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/customers")
    @Operation(summary = "고객 목록 조회", description = "특정 호텔을 이용한 고객 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getCustomers(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 고객 목록 조회
        List<CustomerListDto> customers = customerService.findCustomersByContentId(contentid);
        
        map.put("success", true);
        map.put("customers", customers);
        
        return ResponseEntity.ok(map);
    }

    @GetMapping("/customerHistory")
    @Operation(summary = "고객 이용 이력 조회", description = "특정 호텔의 고객 이용 이력과 리뷰를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getCustomerHistory(
        @Parameter(description = "고객 ID 검색어", example = "user123")
        @RequestParam(value = "customerId", required = false) String customerId,
        @Parameter(description = "상태 필터 (null: 전체, 2: 취소, 4: 완료)", example = "4")
        @RequestParam(value = "status", required = false) Integer statusFilter,
        @Parameter(description = "평점 필터 (null: 전체, 1-5: 해당 평점)", example = "5")
        @RequestParam(value = "rating", required = false) Integer ratingFilter,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 고객 이용 이력 조회
        List<CustomerHistoryDto> history = roomReservationService.findCustomerHistory(
            contentid, customerId, statusFilter, ratingFilter);
        
        map.put("success", true);
        map.put("history", history);
        
        return ResponseEntity.ok(map);
    }

    @GetMapping("/customerHistoryStats")
    @Operation(summary = "고객 이용 이력 통계 조회", description = "특정 호텔의 평균 평점과 피드백 갯수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getCustomerHistoryStats(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 통계 계산
        java.math.BigDecimal averageRating = reviewService.getAverageRatingByContentId(contentid);
        Long feedbackCount = reviewService.getFeedbackCountByContentId(contentid);
        Long totalHistoryCount = roomReservationService.countTotalHistoryByContentId(contentid);
        
        CustomerHistoryStatsDto stats = new CustomerHistoryStatsDto();
        stats.setAverageRating(averageRating != null ? averageRating : java.math.BigDecimal.ZERO);
        stats.setFeedbackCount(feedbackCount != null ? feedbackCount : 0L);
        stats.setTotalHistoryCount(totalHistoryCount != null ? totalHistoryCount : 0L);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/feedback")
    @Operation(summary = "피드백 목록 조회", description = "특정 호텔의 리뷰 피드백 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getFeedbacks(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 피드백 목록 조회
        List<FeedbackDto> feedbacks = reviewService.getFeedbacksByContentId(contentid);
        
        map.put("success", true);
        map.put("feedbacks", feedbacks);
        
        return ResponseEntity.ok(map);
    }

    @GetMapping("/feedbackStats")
    @Operation(summary = "피드백 통계 조회", description = "특정 호텔의 피드백 통계를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getFeedbackStats(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        // 통계 조회
        FeedbackStatsDto stats = reviewService.getFeedbackStats(contentid);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/feedback/{reviewIdx}/answer")
    @Operation(summary = "리뷰 답변 작성", description = "특정 리뷰에 대한 답변을 작성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 작성됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> createReviewAnswer(
        @Parameter(description = "리뷰 고유번호")
        @PathVariable Integer reviewIdx,
        @Parameter(description = "답변 내용")
        @RequestBody Map<String, String> requestBody,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        try {
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
            String content = requestBody.get("content");
            if (content == null || content.trim().isEmpty()) {
            map.put("success", false);
                map.put("message", "답변 내용을 입력해주세요.");
            return ResponseEntity.badRequest().body(map);
        }
        
            // 답변 작성
            ReviewAnswer answer = reviewService.createReviewAnswer(reviewIdx, adminIdx, content);
        
        map.put("success", true);
            map.put("message", "답변이 작성되었습니다.");
            map.put("answer", answer);
        
        return ResponseEntity.ok(map);
        } catch (RuntimeException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }
    }

    @PutMapping("/feedback/answer/{reviewAnswerIdx}")
    @Operation(summary = "리뷰 답변 수정", description = "작성한 리뷰 답변을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 수정됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateReviewAnswer(
        @Parameter(description = "답변 고유번호")
        @PathVariable Integer reviewAnswerIdx,
        @Parameter(description = "답변 내용")
        @RequestBody Map<String, String> requestBody,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        try {
        // JWT에서 adminIdx 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        if (adminIdx == null) {
                map.put("success", false);
                map.put("message", "인증 정보가 유효하지 않습니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            String content = requestBody.get("content");
            if (content == null || content.trim().isEmpty()) {
                map.put("success", false);
                map.put("message", "답변 내용을 입력해주세요.");
                return ResponseEntity.badRequest().body(map);
        }
        
            // 답변 수정
            ReviewAnswer answer = reviewService.updateReviewAnswer(reviewAnswerIdx, adminIdx, content);
            
            map.put("success", true);
            map.put("message", "답변이 수정되었습니다.");
            map.put("answer", answer);
            
            return ResponseEntity.ok(map);
        } catch (RuntimeException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }
    }

    @DeleteMapping("/feedback/answer/{reviewAnswerIdx}")
    @Operation(summary = "리뷰 답변 삭제", description = "작성한 리뷰 답변을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 삭제됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> deleteReviewAnswer(
        @Parameter(description = "답변 고유번호")
        @PathVariable Integer reviewAnswerIdx,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        try {
            // JWT에서 adminIdx 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
            Integer adminIdx = principal.getAdminIdx();
            if (adminIdx == null) {
                map.put("success", false);
                map.put("message", "인증 정보가 유효하지 않습니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            // 답변 삭제
            reviewService.deleteReviewAnswer(reviewAnswerIdx, adminIdx);
            
            map.put("success", true);
            map.put("message", "답변이 삭제되었습니다.");
            
            return ResponseEntity.ok(map);
        } catch (RuntimeException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }
    }

    @GetMapping("/hotelInfoForEdit")
    @Operation(summary = "호텔 정보 조회 (수정용)", description = "정규화된 테이블에서 호텔 정보를 조회하여 등록 폼 구조로 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "호텔 정보 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getHotelInfoForEdit(
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        try {
            // 정규화된 테이블에서 호텔 정보 조회하여 등록 폼 구조로 변환
            com.sist.backend.dto.admin.HotelEditFormDto dto = hotelInfoService.getHotelInfoForEdit(contentid);
            
            map.put("success", true);
            map.put("data", dto);
            
            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "호텔 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(map);
        }
    }

    @PutMapping("/hotelInfoForEdit")
    @Operation(summary = "호텔 정보 수정", description = "등록 폼 구조로 받은 데이터를 정규화된 테이블에 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 수정됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "호텔 정보 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateHotelInfo(
        @RequestBody com.sist.backend.dto.admin.HotelEditFormDto dto,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request) {

        Map<String, Object> map = new HashMap<>();

        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }

        try {
            // 정규화된 테이블에 저장
            hotelInfoService.updateHotelInfo(contentid, dto);

            map.put("success", true);
            map.put("message", "호텔 정보가 성공적으로 수정되었습니다.");

            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "호텔 정보 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(map);
    }
    }

}
