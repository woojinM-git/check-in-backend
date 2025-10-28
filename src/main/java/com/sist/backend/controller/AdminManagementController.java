package com.sist.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @Operation(summary = "최근 예약 현황", description = "최근 예약 현황을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<RoomReservationDto>> recentReservations(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(value = "page", defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "5") 
        @RequestParam(value = "size", defaultValue = "5") int size,
        @Parameter(description = "업체 ID", example = "1003654")
        @RequestParam(value = "contentid", defaultValue = "1003654") String contentid){
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
        @Parameter(description = "업체 ID", example = "1003654")
        @RequestParam(value = "contentid", defaultValue = "1003654") String contentid){
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
        @Parameter(description = "업체 ID", example = "1003654")
        @RequestParam(value = "contentid", defaultValue = "1003654") String contentid){
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(roomReservationService.findCheckoutPendingWithDetails(contentid, pageable));
    }

    @RequestMapping("/roomList")
    @Operation(summary = "방 목록", description = "방 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<Room>> findByContentIdAdmin(
        @Parameter(description = "업체 ID", example = "1003654")
        @RequestParam(value = "contentid", defaultValue = "1003654") String contentid){
        return ResponseEntity.ok(roomService.findByContentIdAdmin(contentid));
    }

    @RequestMapping("/couponIssue")
    @Operation(summary = "쿠폰 발급 화면", description = "쿠폰 발급 화면을 보여줍니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> findByContentIdAdmin(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size,
            @Parameter(description = "현재 로그인한 관리자 ID", example = "1")
            @RequestParam(value = "adminIdx", defaultValue = "1") Integer adminIdx){
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Map<String, Object> map = new HashMap<>();
        map.put("couponTemplates", couponTemplateService.findByStatus());
        map.put("coupons", couponService.findByAdminIdx(adminIdx, pageable));
        return ResponseEntity.ok(map);
    }
    
    @RequestMapping("/customerSearch")
    @Operation(summary = "고객 검색", description = "이름, 이메일, 닉네임으로 고객을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<Customer>> searchCustomers(
        @Parameter(description = "검색어 (닉네임)", example = "jiwo")
        @RequestParam(value = "searchTerm", defaultValue = "") String searchTerm){
        
        List<Customer> customers;
        if (searchTerm.trim().isEmpty()) {
            // 검색어가 없으면 빈 리스트 반환
            customers = List.of();
        } else {
            // 검색어가 있으면 통합 검색
            customers = customerService.findByNicknameContaining(searchTerm);
        }
        
        return ResponseEntity.ok(customers);
    }

    @PostMapping("/couponCreate")
    @Operation(summary = "쿠폰 생성", description = "관리자가 고객에게 쿠폰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰이 성공적으로 생성됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> createCoupon(@RequestBody Map<String, Object> request) {
        try {
            Integer templateIdx = (Integer) request.get("templateIdx");
            Integer customerIdx = (Integer) request.get("customerIdx");
            Integer adminIdx = 1; // 임시로 관리자 ID 1로 설정 (실제로는 세션에서 가져와야 함)
            
            Coupon createdCoupon = couponService.createCoupon(templateIdx, customerIdx, adminIdx);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "쿠폰이 성공적으로 발급되었습니다.");
            response.put("coupon", createdCoupon);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "쿠폰 발급 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
