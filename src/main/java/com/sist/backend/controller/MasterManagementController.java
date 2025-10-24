package com.sist.backend.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sist.backend.dto.master.CustomerDto;
import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.dto.master.RegistrationRequestDto;
import com.sist.backend.dto.master.RegistrationRequestPlusDto;
import com.sist.backend.entity.CouponTemplate;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RegistrationRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.CouponTemplateService;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.service.RegistrationRequestService;
import com.sist.backend.service.RoomPaymentService;
import com.sist.backend.service.RoomReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterManagementController {
    
    /* 서비스 호출 */
    private final HotelInfoService hotelInfoService;
    private final RoomPaymentService roomPaymentService;
    private final RegistrationRequestService registrationRequestService;
    private final CustomerService customerService;
    private final CouponTemplateService couponTemplateService;
    private final RoomReservationService roomReservationService;

   
    /* 등록되어 있는 회원의 목록 */
    @GetMapping("/customers")
    @Operation(summary = "마스터 회원 관리", description = "등록되어 있는 회원의 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<CustomerDto>>  findCustomerAndRank(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(customerService.findCustomerAndRankDto(pageable));
    }

    @GetMapping("/hotels")
    @Operation(summary = "마스터 호텔 관리", description = "등록되어 있는 호텔의 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<HotelInfoDto>>  findAllHotelWithDetailsAsDto(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(hotelInfoService.findAllHotelWithDetailsAsDto(pageable));
    }

    /* 승인요청을 한 호텔들 */
    @RequestMapping("/hotelApproval")
    @Operation(summary = "승인요청 관리", description = "승인요청을 한 호텔 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> findAllHotelWithDetailsDto(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<RegistrationRequestPlusDto> requests = registrationRequestService.findByStatusDto(pageable);
        
        // 통계 정보 추가
        Integer todayApprovedCount = registrationRequestService.findTodayApprovedCount();
        Integer todayRejectedCount = registrationRequestService.findTodayRejectedCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", requests.getContent());
        response.put("totalElements", requests.getTotalElements());
        response.put("totalPages", requests.getTotalPages());
        response.put("number", requests.getNumber());
        response.put("size", requests.getSize());
        response.put("todayApprovedCount", todayApprovedCount);
        response.put("todayRejectedCount", todayRejectedCount);
        
        return ResponseEntity.ok(response);
    }

    /* 대시보드 */
    @RequestMapping("/dashboard")
    @Operation(summary = "대시보드 마스터", description = "마스터 대시보드 화면")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> dashboard() {
        /* 대시보드 상단 */
        int HotelCount = hotelInfoService.findRegistrationHotelCount();
        int pendingCount = roomReservationService.findByTodayCount();
        int CustomerCount = customerService.findRegistrationCustomerCount();
        Long paymentAmount = roomPaymentService.findByPrice();
        /* 승인요청 호텔, 고객 목록 */
        List<RegistrationRequestDto> pendingRequests = registrationRequestService.findTop5ByStatusInDashboard();
        int pendingRequestCount = registrationRequestService.findByStatusCount();
        List<Customer> newCustomers = customerService.findByJoinDate();
        
        Map<String, Object> map = new HashMap<>();

        map.put("hotelCount", HotelCount);
        map.put("pendingCount", pendingCount);
        map.put("customerCount", CustomerCount);
        map.put("paymentAmount", paymentAmount);
        map.put("pendingRequests", pendingRequests);
        map.put("pendingRequestCount", pendingRequestCount);
        map.put("newCustomers", newCustomers);

        return ResponseEntity.ok(map);
    }

    /* 쿠폰 템플릿 관리 */
    @RequestMapping("/couponTemplates")
    @Operation(summary = "쿠폰 템플릿 관리", description = "쿠폰 템플릿 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<CouponTemplate>> findAll() {
        return ResponseEntity.ok(couponTemplateService.findByStatus());
    }

    @PostMapping("/createTemplate")
    @Operation(summary = "쿠폰 템플릿 생성", description = "쿠폰 템플릿을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 생성됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CouponTemplate> createTemplate(@RequestBody Map<String, Object> request) {
        try {
            String templateName = (String) request.get("templateName");
            Integer discount = (Integer) request.get("discount");
            Integer validDays = (Integer) request.get("validDays");
            Integer status = (Integer) request.get("status");
            Integer adminIdx = (Integer) request.get("adminIdx");

            CouponTemplate couponTemplate = new CouponTemplate();
            couponTemplate.setTemplateName(templateName);
            couponTemplate.setDiscount(discount);
            couponTemplate.setValidDays(validDays);
            couponTemplate.setStatus(status);
            couponTemplate.setAdminIdx(adminIdx);
            couponTemplate.setCreatedAt(LocalDateTime.now());
            couponTemplate.setUpdatedAt(LocalDateTime.now());

            return ResponseEntity.ok(couponTemplateService.createTemplate(couponTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/approveHotel")
    @Operation(summary = "호텔 승인", description = "호텔 등록 요청을 승인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 승인됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> approveHotel(@RequestBody Map<String, Object> request) {
        try {
            Integer registrationIdx = (Integer) request.get("registrationIdx");
            
            RegistrationRequest registrationRequest = registrationRequestService.findById(registrationIdx);
            registrationRequest.setStatus(1); // 승인 상태로 변경
            registrationRequest.setRegiDate(LocalDateTime.now()); // 승인일 업데이트
            
            RegistrationRequest updatedRequest = registrationRequestService.updateRequest(registrationRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "호텔이 승인되었습니다.");
            response.put("request", updatedRequest);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔 승인 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/rejectHotel")
    @Operation(summary = "호텔 거부", description = "호텔 등록 요청을 거부합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 거부됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> rejectHotel(@RequestBody Map<String, Object> request) {
        try {
            Integer registrationIdx = (Integer) request.get("registrationIdx");
            
            RegistrationRequest registrationRequest = registrationRequestService.findById(registrationIdx);
            registrationRequest.setStatus(2); // 거부 상태로 변경
            registrationRequest.setRegiDate(LocalDateTime.now()); // 거부일 업데이트
            
            RegistrationRequest updatedRequest = registrationRequestService.updateRequest(registrationRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "호텔이 거부되었습니다.");
            response.put("request", updatedRequest);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔 거부 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/updateTemplate")
    @Operation(summary = "쿠폰 템플릿 상태 변경", description = "쿠폰 템플릿의 상태를 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 변경됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateTemplateStatus(@RequestBody Map<String, Object> request) {
        try {
            Integer templateIdx = (Integer) request.get("templateIdx");
            
            CouponTemplate template = couponTemplateService.findById(templateIdx);
            template.setStatus(2); // 삭제 상태로 변경
            template.setUpdatedAt(LocalDateTime.now());
            
            CouponTemplate updatedTemplate = couponTemplateService.updateTemplate(templateIdx, template);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 삭제되었습니다.");
            response.put("template", updatedTemplate);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "템플릿 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}