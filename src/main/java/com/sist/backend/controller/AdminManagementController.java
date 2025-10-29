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
import com.sist.backend.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.sist.backend.dto.admin.CouponCreateDto;
import com.sist.backend.dto.admin.RoomReservationDto;
import com.sist.backend.entity.Room;
import com.sist.backend.service.RoomPaymentService;
import com.sist.backend.service.RoomReservationService;
import com.sist.backend.service.RoomService;
import com.sist.backend.service.hotel.HotelInfoService;

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
    private final RoomService roomService;
    private final CouponTemplateService couponTemplateService;
    private final CustomerService customerService;
    private final CouponService couponService;
    private final HotelInfoService hotelInfoService;
    private final JwtUtils jwtUtils;

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
    public Map<String, Object> dashboard(
        @Parameter(description = "HTTP 요청", hidden = true) 
        HttpServletRequest request){
        Map<String, Object> map = new HashMap<>();
        
        // JWT에서 adminIdx 추출
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return map;
        }
        
        // adminIdx로 contentId 조회
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        String contentid = contentIdOpt.orElse("1003654"); // 기본값
        
        // 오늘 체크인한 사람의 수
        Integer todayCheckinCount = roomReservationService.getTodayCheckinCount();
        map.put("todayCheckinCount", todayCheckinCount != null ? todayCheckinCount : 0);
        // 오늘 체크아웃한 사람의 수
        Integer todayCheckoutCount = roomReservationService.getTodayCheckoutCount();
        map.put("todayCheckoutCount", todayCheckoutCount != null ? todayCheckoutCount : 0);
        // 예약 확정 사람 수
        Integer reservationCount = roomReservationService.findByStatus();
        map.put("reservationCount", reservationCount != null ? reservationCount : 0);
        // 이번달 매출
        Long thisMonthSales = roomPaymentService.findByPrice();
        map.put("thisMonthSales", thisMonthSales != null ? thisMonthSales : 0);

        /* 최근 예약 현황 조회 (5개만) - Room과 Customer 정보 포함 */
        List<RoomReservationDto> roomReservationList = roomReservationService.findByStatusWithDetails(contentid);
        map.put("roomReservationList", roomReservationList);
        return map;
    }

    @RequestMapping("/roomReservationList")
    @Operation(summary = "예약 목록 조회", description = "호텔의 모든 예약 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<RoomReservationDto>> recentReservations(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "10") 
        @RequestParam(value = "size", defaultValue = "10") int size,
        @Parameter(description = "HTTP 요청", hidden = true)
        HttpServletRequest request){
        
        // JWT에서 adminIdx 추출
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
        if (adminIdx == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // adminIdx로 contentId 조회
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        String contentid = contentIdOpt.orElse("1003654");
        
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
    public ResponseEntity<Page<RoomReservationDto>> findCheckinPendingWithDetails(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "5") 
        @RequestParam(value = "size", defaultValue = "5") int size,
        @Parameter(description = "업체 ID", example = "1034361")
        @RequestParam(value = "contentid", defaultValue = "1034361") String contentid){
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(roomReservationService.findCheckinPendingWithDetails(contentid, pageable));
    }

    @RequestMapping("/checkoutPendingList")
    @Operation(summary = "체크아웃 현황 현황", description = "체크아웃 현황을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<RoomReservationDto>> findCheckoutPendingWithDetails(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "5") 
        @RequestParam(value = "size", defaultValue = "5") int size,
        @Parameter(description = "업체 ID", example = "1034361")
        @RequestParam(value = "contentid", defaultValue = "1034361") String contentid){
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(roomReservationService.findCheckoutPendingWithDetails(contentid, pageable));
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
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
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
        
        // JWT에서 adminIdx 추출
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        // adminIdx로 contentId 조회
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        String contentid = contentIdOpt.orElse(null);
        
        if (contentid == null) {
            map.put("success", false);
            map.put("message", "호텔 정보를 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(map);
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
        
        // JWT에서 adminIdx 추출
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        // adminIdx로 contentId 조회
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        String contentid = contentIdOpt.orElse(null);
        
        if (contentid == null) {
            map.put("success", false);
            map.put("message", "호텔 정보를 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(map);
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
        
        // JWT에서 adminIdx 추출
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        // adminIdx로 contentId 조회
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        String contentid = contentIdOpt.orElse(null);
        
        if (contentid == null) {
            map.put("success", false);
            map.put("message", "호텔 정보를 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(map);
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
        
        // JWT에서 adminIdx 추출
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(map);
        }
        
        // adminIdx로 contentId 조회
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        String contentid = contentIdOpt.orElse(null);
        
        if (contentid == null) {
            map.put("success", false);
            map.put("message", "호텔 정보를 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(map);
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
        Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
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


}
