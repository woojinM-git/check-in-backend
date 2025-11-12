package com.sist.backend.controller;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sist.backend.dto.master.RejectHotelRequestDto;
import com.sist.backend.dto.master.StopHotelDto;
import com.sist.backend.dto.master.SuspendCustomerDto;
import com.sist.backend.dto.master.SettlementDto;
import com.sist.backend.dto.master.RegistrationRequestDto;
import com.sist.backend.dto.master.RegistrationRequestPlusDto;
import com.sist.backend.entity.CouponTemplate;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelDraft;
import com.sist.backend.entity.RegistrationRequest;

import com.sist.backend.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Admin;
import com.sist.backend.repository.admin.AdminRepository;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.entity.Answer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterManagementController {
    
    /* 서비스 호출 */
    private final HotelInfoService hotelInfoService;
    private final RegistrationRequestService registrationRequestService;
    private final HotelDraftService hotelDraftService;
    private final CustomerService customerService;
    private final CouponTemplateService couponTemplateService;
    private final CouponService couponService;
    private final RoomReservationService roomReservationService;
    private final AdminRepository adminRepository;
    private final ObjectMapper objectMapper;
    private final SettlementService settlementService;
    private final AnswerService answerService;
    private final StatisticsService statisticsService;

    /**
     * 마스터 권한 확인 (type이 false(0)인지 확인)
     * @param request HTTP 요청 (HttpOnly 쿠키 접근용)
     * @return 마스터가 맞으면 null, 아니면 Forbidden 응답
     */
    private ResponseEntity<Map<String, Object>> checkMasterAuthorization(HttpServletRequest request) {
        // JwtFilter에서 이미 쿠키를 읽어 SecurityContext에 인증 정보를 설정했으므로
        // SecurityContextHolder에서 adminIdx를 가져올 수 있습니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("redirect", true);
            errorResponse.put("message", "인증 정보가 없습니다. 로그인 페이지로 이동합니다.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        
        if (adminIdx == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("redirect", true);
            errorResponse.put("message", "관리자 인덱스가 없습니다. 로그인 페이지로 이동합니다.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        // Admin 조회 및 type 확인
        // status = false(0): 활성 상태, status = true(1): 비활성 상태
        Optional<Admin> adminOpt = adminRepository.findByAdminIdxAndStatus(adminIdx, false);
        if (adminOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("redirect", true);
            errorResponse.put("message", "관리자 정보를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        Admin admin = adminOpt.get();
        // type이 false(0)이면 마스터, true(1)이면 사업자
        // 마스터만 접근 가능하므로 type이 false가 아니면 거부
        if (admin.getType() != null && admin.getType()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("redirect", true);
            errorResponse.put("message", "마스터 권한이 필요합니다. 메인 화면으로 이동합니다.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        // 마스터 권한 확인 완료
        return null;
    }

   
    /* 등록되어 있는 회원의 목록 */
    @GetMapping("/customers")
    @Operation(summary = "마스터 회원 관리", description = "등록되어 있는 회원의 목록을 검색 및 필터링하여 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findCustomerAndRank(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "10") 
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "검색어 (회원명, 이메일, 전화번호)", example = "홍길동") 
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @Parameter(description = "상태 필터 (all, active, inactive, suspended)", example = "all") 
            @RequestParam(value = "statusFilter", defaultValue = "all") String statusFilter,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        
        // 검색어나 필터가 있으면 검색 API 사용, 없으면 기존 API 사용
        if ((searchTerm != null && !searchTerm.trim().isEmpty()) || 
            (statusFilter != null && !statusFilter.equals("all"))) {
            return ResponseEntity.ok(customerService.searchCustomers(searchTerm, statusFilter, pageable));
        } else {
            return ResponseEntity.ok(customerService.findCustomerAndRankDto(pageable));
        }
    }

    @GetMapping("/hotels")
    @Operation(summary = "마스터 호텔 관리", description = "등록되어 있는 호텔의 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findAllHotelWithDetailsAsDto(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size,
            @Parameter(description = "검색어 (호텔명, 사업자명, 위치)", example = "서울") 
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(hotelInfoService.findAllHotelWithDetailsAsDto(search, pageable));
    }

    /* 승인요청을 한 호텔들 */
    @RequestMapping("/hotelApproval")
    @Operation(summary = "승인요청 관리", description = "승인요청을 한 호텔 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findAllHotelWithDetailsDto(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
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

    /* 승인요청 상세 조회 (기본 정보만 - 빠른 로딩) */
    @GetMapping("/hotelApproval/{registrationIdx}")
    @Operation(summary = "승인요청 상세 조회 (기본 정보)", description = "특정 호텔 승인 요청의 기본 정보만 조회합니다. (images, rooms, dining 제외)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getHotelApprovalDetail(
            @Parameter(description = "등록 요청 ID", example = "1") 
            @PathVariable Integer registrationIdx,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            // 1. RegistrationRequest 조회
            RegistrationRequest registrationRequest = registrationRequestService.findById(registrationIdx);
            
            // 2. HotelDraft 조회
            Integer draftIdx = registrationRequest.getDraftIdx();
            if (draftIdx == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "임시저장 데이터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Optional<HotelDraft> draftOpt = hotelDraftService.findById(draftIdx);
            if (draftOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "임시저장 데이터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            HotelDraft draft = draftOpt.get();
            
            // 3. formData를 Map으로 파싱
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
            
            // 5. 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", basicData);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /* 승인요청 탭별 데이터 조회 (지연 로딩) */
    @GetMapping("/hotelApproval/{registrationIdx}/{tab}")
    @Operation(summary = "승인요청 탭별 데이터 조회", description = "특정 탭(images, rooms, dining)의 데이터만 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getHotelApprovalTabData(
            @Parameter(description = "등록 요청 ID", example = "1") 
            @PathVariable Integer registrationIdx,
            @Parameter(description = "탭 이름", example = "images") 
            @PathVariable String tab,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            // 유효한 탭인지 확인
            if (!tab.equals("images") && !tab.equals("rooms") && !tab.equals("dining")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않은 탭 이름입니다: " + tab);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 1. RegistrationRequest 조회
            RegistrationRequest registrationRequest = registrationRequestService.findById(registrationIdx);
            
            // 2. HotelDraft 조회
            Integer draftIdx = registrationRequest.getDraftIdx();
            if (draftIdx == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "임시저장 데이터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Optional<HotelDraft> draftOpt = hotelDraftService.findById(draftIdx);
            if (draftOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "임시저장 데이터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            HotelDraft draft = draftOpt.get();
            
            // 3. formData를 Map으로 파싱
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(Map.class, String.class, Object.class);
            Map<String, Object> formDataMap = objectMapper.readValue(draft.getFormData(), mapType);
            
            // 4. 해당 탭의 데이터만 추출
            Map<String, Object> tabData = new HashMap<>();
            if (tab.equals("images")) {
                tabData.put("images", formDataMap.getOrDefault("images", List.of()));
                tabData.put("events", formDataMap.getOrDefault("events", List.of()));
            } else if (tab.equals("rooms")) {
                tabData.put("rooms", formDataMap.getOrDefault("rooms", List.of()));
            } else if (tab.equals("dining")) {
                tabData.put("dining", formDataMap.getOrDefault("dining", List.of()));
            }
            
            // 5. 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tabData);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "탭 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 대시보드 */
    @RequestMapping("/dashboard")
    @Operation(summary = "대시보드 마스터", description = "마스터 대시보드 화면")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> dashboard(
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        /* 대시보드 상단 */
        int HotelCount = hotelInfoService.findRegistrationHotelCount();
        int pendingCount = roomReservationService.findByTodayCount();
        int CustomerCount = customerService.findRegistrationCustomerCount();
        // 총 매출은 hotelSettlement의 totalRevenue 합계로 변경
        Long totalRevenue = statisticsService.getTotalRevenueAll();
        /* 승인요청 호텔, 고객 목록 */
        List<RegistrationRequestDto> pendingRequests = registrationRequestService.findTop5ByStatusInDashboard();
        int pendingRequestCount = registrationRequestService.findByStatusCount();
        List<Customer> newCustomers = customerService.findByJoinDate();
        
        Map<String, Object> map = new HashMap<>();

        map.put("hotelCount", HotelCount);
        map.put("pendingCount", pendingCount);
        map.put("customerCount", CustomerCount);
        map.put("paymentAmount", totalRevenue); // totalRevenue로 변경 (하위 호환성을 위해 paymentAmount 키 유지)
        map.put("pendingRequests", pendingRequests);
        map.put("pendingRequestCount", pendingRequestCount);
        map.put("newCustomers", newCustomers);

        return ResponseEntity.ok(map);
    }

    /* 쿠폰 템플릿 관리 */
    @RequestMapping("/couponTemplates")
    @Operation(summary = "쿠폰 템플릿 관리", description = "쿠폰 템플릿 목록을 보여줍니다. type 파라미터로 필터링 가능 (0: 지정발급형식, 1: 단체 발급형식)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findAll(
            @Parameter(description = "쿠폰 타입 (0: 지정발급형식, 1: 단체 발급형식)", required = false)
            @RequestParam(value = "type", required = false) Integer type,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        if (type != null) {
            return ResponseEntity.ok(couponTemplateService.findByStatusAndType(type));
        }
        
        return ResponseEntity.ok(couponTemplateService.findByStatus());
    }

    @GetMapping("/couponTemplates/inactive")
    @Operation(summary = "비활성 쿠폰 템플릿 조회", description = "비활성화된 쿠폰 템플릿 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> findInactiveTemplates(
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        return ResponseEntity.ok(couponTemplateService.findByInactiveStatus());
    }

    @PostMapping("/activateTemplate")
    @Operation(summary = "쿠폰 템플릿 활성화", description = "비활성화된 쿠폰 템플릿을 활성화합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 활성화됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> activateTemplate(
            @RequestBody Map<String, Object> requestData,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            Integer templateIdx = (Integer) requestData.get("templateIdx");
            if (templateIdx == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "템플릿 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            CouponTemplate activatedTemplate = couponTemplateService.activateTemplate(templateIdx);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 성공적으로 활성화되었습니다.");
            response.put("template", activatedTemplate);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "템플릿 활성화 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/createTemplate")
    @Operation(summary = "쿠폰 템플릿 생성", description = "쿠폰 템플릿을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 생성됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> createTemplate(
            @RequestBody Map<String, Object> requestData,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            String templateName = (String) requestData.get("templateName");
            Integer discount = (Integer) requestData.get("discount");
            Integer validDays = (Integer) requestData.get("validDays");
            Integer status = (Integer) requestData.get("status");
            Integer type = (Integer) requestData.get("type");
            Integer adminIdx = (Integer) requestData.get("adminIdx");

            CouponTemplate couponTemplate = new CouponTemplate();
            couponTemplate.setTemplateName(templateName);
            couponTemplate.setDiscount(discount);
            couponTemplate.setValidDays(validDays);
            couponTemplate.setStatus(status);
            couponTemplate.setType(type != null ? type : 0); // 기본값 0 (지정발급형식)
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
    public ResponseEntity<?> approveHotel(
        @RequestBody RejectHotelRequestDto requestData,
        @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            System.out.println("✅ 호텔 승인 요청 수신: registrationIdx=" + requestData.getRegistrationIdx());
            
            // 호텔 승인 처리 (JSON 파싱 및 정규화된 테이블 저장)
            registrationRequestService.approveHotelRegistration(requestData.getRegistrationIdx(), LocalDateTime.now());
            
            System.out.println("✅ 호텔 승인 처리 완료: registrationIdx=" + requestData.getRegistrationIdx());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "호텔이 승인되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ 호텔 승인 처리 실패 (잘못된 요청): " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            System.out.println("❌ 호텔 승인 처리 실패 (서버 오류): " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔 승인 중 오류가 발생했습니다: " + e.getMessage());
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
    public ResponseEntity<?> rejectHotel(
        @RequestBody RejectHotelRequestDto requestData,
        @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            registrationRequestService.updateRejectRequest(requestData.getRegistrationIdx(), requestData.getRefusalMsg(), 2);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "호텔이 거부되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔 거부 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/editTemplate")
    @Operation(summary = "쿠폰 템플릿 수정", description = "쿠폰 템플릿 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 수정됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> editTemplate(
            @RequestBody Map<String, Object> requestData,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            Integer templateIdx = (Integer) requestData.get("templateIdx");
            String templateName = (String) requestData.get("templateName");
            Integer discount = requestData.get("discount") instanceof Number 
                ? ((Number) requestData.get("discount")).intValue() 
                : null;
            Integer validDays = requestData.get("validDays") instanceof Number 
                ? ((Number) requestData.get("validDays")).intValue() 
                : null;
            Integer status = requestData.get("status") instanceof Number 
                ? ((Number) requestData.get("status")).intValue() 
                : null;
            Integer type = requestData.get("type") instanceof Number 
                ? ((Number) requestData.get("type")).intValue() 
                : null;

            CouponTemplate updateData = new CouponTemplate();
            updateData.setTemplateName(templateName);
            updateData.setDiscount(discount);
            updateData.setValidDays(validDays);
            updateData.setStatus(status);
            updateData.setType(type);
            
            CouponTemplate updatedTemplate = couponTemplateService.updateTemplate(templateIdx, updateData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 성공적으로 수정되었습니다.");
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
            errorResponse.put("message", "템플릿 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/delTemplate")
    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 삭제됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "템플릿을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> deleteTemplate(
            @RequestBody Map<String, Object> requestData,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            Integer templateIdx = (Integer) requestData.get("templateIdx");
            
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

    @PostMapping("/suspendHotel")
    @Operation(summary = "호텔 정지", description = "호텔을 정지 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 정지됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "호텔을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> suspendHotel(
        @RequestBody StopHotelDto requestData,
        @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }
        
        try {
            hotelInfoService.suspendHotel(requestData.getContentId(), requestData.getReason());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "호텔이 정지되었습니다.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔 정지 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 1대1 문의 답변 작성
     */
    @PostMapping("/inquiry/{centerIdx}/answer")
    @Operation(summary = "1대1 문의 답변 작성", description = "특정 문의에 대한 답변을 작성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 작성됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> createInquiryAnswer(
            @Parameter(description = "문의 고유번호")
            @PathVariable Integer centerIdx,
            @Parameter(description = "답변 내용")
            @RequestBody Map<String, String> requestBody,
            @Parameter(description = "HTTP 요청", hidden = true)
            HttpServletRequest request) {

        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

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
            Answer answer = answerService.createAnswer(centerIdx, adminIdx, content);

            map.put("success", true);
            map.put("message", "답변이 작성되었습니다.");
            map.put("answer", answer);

            return ResponseEntity.ok(map);
        } catch (RuntimeException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(map);
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "답변 작성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(map);
        }
    }

    @PostMapping("/suspendCustomer")
    @Operation(summary = "회원 정지", description = "회원을 정지 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 정지됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> suspendCustomer(
        @RequestBody SuspendCustomerDto requestData,
        @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            customerService.suspendCustomer(requestData.getCustomerIdx());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원이 정지되었습니다.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원 정지 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/batchUpdateCustomers")
    @Operation(summary = "회원 일괄 처리", description = "선택한 회원들을 일괄적으로 활성화/비활성화/정지 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 처리됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> batchUpdateCustomers(
        @RequestBody Map<String, Object> requestData,
        @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> customerIdxList = (List<Integer>) requestData.get("customerIdxList");
            String action = (String) requestData.get("action");

            if (customerIdxList == null || customerIdxList.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "선택한 회원이 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (action == null || action.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "처리할 작업을 지정해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Integer status;
            String actionName;
            switch (action) {
                case "activate":
                    status = 0;
                    actionName = "활성화";
                    break;
                case "deactivate":
                case "suspend":
                    status = 1;
                    actionName = action.equals("suspend") ? "정지" : "비활성화";
                    break;
                default:
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "지원하지 않는 작업입니다: " + action);
                    return ResponseEntity.badRequest().body(errorResponse);
            }

            int successCount = customerService.batchUpdateCustomerStatus(customerIdxList, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("%d명의 회원이 %s 처리되었습니다.", successCount, actionName));
            response.put("successCount", successCount);
            response.put("totalCount", customerIdxList.size());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원 일괄 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 정산 목록 조회 */
    @GetMapping("/settlements")
    @Operation(summary = "정산 목록 조회", description = "호텔별 정산 내역을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getSettlements(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지당 데이터 개수", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            Pageable pageable = Pageable.ofSize(size).withPage(page);
            Page<SettlementDto> settlements = settlementService.findAllSettlements(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", settlements.getContent());
            response.put("totalElements", settlements.getTotalElements());
            response.put("totalPages", settlements.getTotalPages());
            response.put("currentPage", settlements.getNumber());
            response.put("size", settlements.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "정산 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 정산 데이터 생성 */
    @PostMapping("/settlements/create")
    @Operation(summary = "정산 데이터 생성", description = "지정한 월의 정산 데이터를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 생성됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> createSettlement(
            @Parameter(description = "연도", example = "2024")
            @RequestParam(value = "year", required = false) Integer year,
            @Parameter(description = "월 (1-12)", example = "1")
            @RequestParam(value = "month", required = false) Integer month,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            if (year != null && month != null) {
                settlementService.createSettlementForMonth(year, month);
            } else {
                // year와 month가 없으면 직전 달 정산 생성
                settlementService.createSettlementForPreviousMonth();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "정산 데이터가 생성되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "정산 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 통계 데이터 조회 */
    @GetMapping("/statistics")
    @Operation(summary = "통계 데이터 조회", description = "날짜 범위별 통계 데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getStatistics(
            @Parameter(description = "날짜 범위 (week: 7일, month: 30일, quarter: 3개월, year: 1년)", example = "month")
            @RequestParam(value = "dateRange", defaultValue = "month") String dateRange,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            Map<String, Object> statistics = statisticsService.getStatistics(dateRange);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "통계 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 월별 수수료 수익 조회 (차트용) */
    @GetMapping("/statistics/monthlyCommission")
    @Operation(summary = "월별 수수료 수익 조회", description = "최근 12개월간의 월별 수수료 수익을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getMonthlyCommissionRevenue(
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            List<Map<String, Object>> monthlyData = statisticsService.getMonthlyCommissionRevenue();
            YearMonth serviceStartMonth = statisticsService.getSiteServiceStartMonth();
            
            Map<String, Object> response = new HashMap<>();
            response.put("monthlyData", monthlyData);
            response.put("minYear", serviceStartMonth.getYear());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "월별 수수료 수익 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 지역별 통계 조회 */
    @GetMapping("/statistics/region")
    @Operation(summary = "지역별 통계 조회", description = "호텔 수가 많은 상위 4개 지역의 통계를 조회합니다. (캐싱 적용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getRegionStatistics(
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            List<Map<String, Object>> regionStats = statisticsService.getRegionStatistics();
            return ResponseEntity.ok(regionStats);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "지역별 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 회원 등급별 통계 조회 */
    @GetMapping("/statistics/memberGrade")
    @Operation(summary = "회원 등급별 통계 조회", description = "각 등급별 인원수, 비율, 평균 지출액을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getMemberGradeStatistics(
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            List<Map<String, Object>> memberGradeStats = statisticsService.getMemberGradeStatistics();
            return ResponseEntity.ok(memberGradeStats);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원 등급별 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 호텔별 매출 순위 조회 */
    @GetMapping("/statistics/hotelRankings")
    @Operation(summary = "호텔별 매출 순위 조회", description = "최근 30일 기준 호텔별 매출 순위를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getHotelRevenueRankings(
            @Parameter(description = "상위 N개 호텔 (기본값: 4)", required = false) @RequestParam(required = false) Integer limit,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            List<Map<String, Object>> hotelRankings = statisticsService.getHotelRevenueRankings(limit);
            return ResponseEntity.ok(hotelRankings);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "호텔별 매출 순위 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 쿠폰 일괄 발급 */
    @PostMapping("/coupon-batch/issue")
    @Operation(summary = "등급별 쿠폰 일괄 발급", description = "선택한 등급의 모든 활성 고객에게 쿠폰을 일괄 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 발급됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> batchIssueCoupons(
            @Parameter(description = "등급", required = true) @RequestParam String rank,
            @Parameter(description = "쿠폰 템플릿 번호", required = true) @RequestParam Integer templateIdx,
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            // 마스터 adminIdx 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
            Integer adminIdx = principal.getAdminIdx();

            if (adminIdx == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "관리자 인덱스를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 등급별 쿠폰 일괄 발급
            int issuedCount = couponService.batchIssueCouponsByRank(rank, templateIdx, adminIdx);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("등급 '%s'에 해당하는 %d명의 고객에게 쿠폰이 발급되었습니다.", rank, issuedCount));
            response.put("issuedCount", issuedCount);
            response.put("rank", rank);
            response.put("templateIdx", templateIdx);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "쿠폰 일괄 발급 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /* 등급별 인원수 조회 */
    @GetMapping("/coupon-batch/rankCounts")
    @Operation(summary = "등급별 인원수 조회", description = "모든 등급별 활성 고객 수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getRankCustomerCounts(
            @Parameter(description = "HTTP 요청", hidden = true) HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> authCheck = checkMasterAuthorization(request);
        if (authCheck != null) {
            return authCheck;
        }

        try {
            Map<String, Long> rankCounts = couponService.getAllRankCustomerCounts();
            return ResponseEntity.ok(rankCounts);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "등급별 인원수 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}