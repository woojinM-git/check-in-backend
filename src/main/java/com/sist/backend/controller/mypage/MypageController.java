package com.sist.backend.controller.mypage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sist.backend.dto.mypage.ReservationResponseDTO;
import com.sist.backend.dto.mypage.WritableReviewDTO;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.CouponTemplate;
import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.mypage.MyPageService;
import com.sist.backend.repository.CouponRepository;
import com.sist.backend.repository.CouponTemplateRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
@Tag(name="마이페이지", description="마이페이지 관련 API")
public class MypageController {

    private final MyPageService myPageService;
    private final CustomerService customerService;
    private final CouponRepository couponRepository;
    private final CouponTemplateRepository couponTemplateRepository;

    /* 
     * 마이페이지 예약 내역 조회 API
     * 엔드포인트: GET /api/mypage/reservations?status={status}
     */

    /* 예약 내역 조회 */
    @GetMapping("/reservations")
    @Operation(summary="예약 내역 조회", description="예약 내역을 조회합니다. (페이지네이션 지원)")
    public ResponseEntity<?> getReservations(
            @RequestParam(name = "status") String status,
            @RequestParam(name = "type", defaultValue = "hotel") String type, // hotel 또는 dining
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "3") int size,
            HttpServletRequest request) {

        // JWT에서 사용자 정보 가져오기
        Integer customerIdx = getCustomerIdxFromToken(request);

        if (customerIdx == null) {
            //인증 정보가 없을 경우 401 에러 반환
            return ResponseEntity.status(401).body(Map.of(
                "message", "인증 정보가 유효하지 않습니다."));
        }

        System.out.println("👤 예약 내역 조회 - customerIdx: " 
            + customerIdx 
            + ", status: " 
            + status 
            + ", type: "
            + type
            + ", page: " 
            + page + ", size: " 
            + size);

        // 타입에 따라 다른 서비스 호출
        if ("dining".equals(type)) {
            // 다이닝 예약 조회
            org.springframework.data.domain.Page<com.sist.backend.dto.mypage.DiningReservationResponseDTO> reservationsPage = 
                myPageService.getMyDiningReservationsByStatus(customerIdx, status, page, size);

            return ResponseEntity.ok(Map.of(
                "reservations", reservationsPage.getContent(),
                "totalElements", reservationsPage.getTotalElements(),
                "totalPages", reservationsPage.getTotalPages(),
                "number", reservationsPage.getNumber(),
                "size", reservationsPage.getSize()
            ));
        } else {
            // 호텔 예약 조회 (기존 로직)
            org.springframework.data.domain.Page<ReservationResponseDTO> reservationsPage = 
                myPageService.getMyReservationsByStatus(customerIdx, status, page, size);

            // Spring Boot Page 객체를 프론트엔드가 기대하는 형식으로 변환
            return ResponseEntity.ok(Map.of(
                "reservations", reservationsPage.getContent(),
                "totalElements", reservationsPage.getTotalElements(),
                "totalPages", reservationsPage.getTotalPages(),
                "number", reservationsPage.getNumber(),
                "size", reservationsPage.getSize()
            ));
        }
    }

    /* 예약 상세 조회 */
    @GetMapping("/reservations/{reservationId}")
    @Operation(summary="예약 상세 조회", description="예약 상세 정보를 조회합니다.")
    public ResponseEntity<?> getReservationDetail(
            @PathVariable Integer reservationId,
            @RequestParam(name = "type", defaultValue = "hotel") String type, // hotel 또는 dining
            HttpServletRequest request) {
        
        // JWT에서 사용자 정보 가져오기
        Integer customerIdx = getCustomerIdxFromToken(request);

        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of(
                "message", "인증 정보가 유효하지 않습니다."));
        }

        try {
            System.out.println("👤 예약 상세 조회 - customerIdx: " + customerIdx + ", reservationId: " + reservationId + ", type: " + type);
            
            // 타입에 따라 다른 서비스 호출
            if ("dining".equals(type)) {
                // 다이닝 예약 상세 조회
                com.sist.backend.dto.mypage.DiningReservationResponseDTO reservation = 
                    myPageService.getDiningReservationDetail(reservationId, customerIdx);
                
                if (reservation == null) {
                    return ResponseEntity.status(404).body(Map.of(
                        "message", "예약 정보를 찾을 수 없습니다."));
                }
                
                return ResponseEntity.ok(reservation);
            } else {
                // 호텔 예약 상세 조회 (기존 로직)
                ReservationResponseDTO reservation = myPageService.getReservationDetail(reservationId, customerIdx);
                
                if (reservation == null) {
                    return ResponseEntity.status(404).body(Map.of(
                        "message", "예약 정보를 찾을 수 없습니다."));
                }
                
                return ResponseEntity.ok(reservation);
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "message", "예약 상세 조회 중 오류가 발생했습니다.",
                "error", e.getMessage()));
        }
    }
    /**
     * 작성 가능한 리뷰 조회 (이용완료된 예약 중 아직 리뷰를 작성하지 않은 것)
     */
    @GetMapping("/writable-reviews")
    @Operation(summary="작성 가능한 리뷰 조회", description="작성 가능한 리뷰를 조회합니다.")
    public ResponseEntity<?> getWritableReviews(HttpServletRequest request) {
        try {
            // JWT에서 사용자 정보 가져오기
            Integer customerIdx = getCustomerIdxFromToken(request);
            
            if (customerIdx == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증 정보가 유효하지 않습니다."));
            }
            
            // 작성 가능한 리뷰 목록 조회
            List<WritableReviewDTO> writableReviews = myPageService.getWritableReviews(customerIdx);
            
            return ResponseEntity.ok(Map.of("reviews", writableReviews));
            
        } catch (Exception e) {
            System.out.println("❌ 작성 가능한 리뷰 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "message", "작성 가능한 리뷰 조회 중 오류가 발생했습니다.",
                "error", e.getMessage()));
        }
    }

    /* 프로필 정보 조회 */
    @GetMapping("/profile")
    @Operation(summary="프로필 정보 조회", description="프로필 정보를 조회합니다.")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            // JWT에서 사용자 정보 가져오기
            Integer customerIdx = getCustomerIdxFromToken(request);
            
            if (customerIdx == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증 정보가 유효하지 않습니다."));
            }

            // 2. 고객 정보 조회
            Customer customer = customerService.findByCustomerIdx(customerIdx)
                .orElse(null);
            
            if (customer == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "message", "사용자 정보를 찾을 수 없습니다."));
            }

            // 3. 프로필 정보 반환 (비밀번호 제외)
            return ResponseEntity.ok(customer);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "message", "프로필 조회 중 오류가 발생했습니다.",
                "error", e.getMessage()));
        }
    }

    /**
     * 고객 보유 쿠폰 전체 이력 조회
     */
    @GetMapping("/coupons")
    @Operation(summary="내 쿠폰 이력 조회", description="사용 가능/사용 완료/기간 만료 포함 전체 쿠폰 이력을 조회합니다.")
    public ResponseEntity<?> getMyCoupons(HttpServletRequest request) {
        Integer customerIdx = getCustomerIdxFromToken(request);

        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of(
                "message", "인증 정보가 유효하지 않습니다."
            ));
        }

        List<Coupon> coupons = couponRepository.findByCustomerIdx(customerIdx);
        Set<Integer> templateIds = coupons.stream()
            .map(Coupon::getTemplateIdx)
            .filter(templateIdx -> templateIdx != null)
            .collect(Collectors.toSet());
        Map<Integer, CouponTemplate> templateMap = couponTemplateRepository.findAllById(templateIds).stream()
            .collect(Collectors.toMap(CouponTemplate::getTemplateIdx, template -> template));
        List<Map<String, Object>> data = coupons.stream()
            .map(coupon -> {
                Map<String, Object> map = new HashMap<>();
                map.put("couponIdx", coupon.getCouponIdx());
                map.put("templateIdx", coupon.getTemplateIdx());
                map.put("customerIdx", coupon.getCustomerIdx());
                map.put("adminIdx", coupon.getAdminIdx());
                map.put("createDate", coupon.getCreateDate());
                map.put("endDate", coupon.getEndDate());
                map.put("status", coupon.getStatus() != null && coupon.getStatus() ? 1 : 0);
                CouponTemplate template = coupon.getTemplateIdx() != null
                    ? templateMap.get(coupon.getTemplateIdx())
                    : null;
                map.put("templateName", template != null ? template.getTemplateName() : "");
                map.put("discount", template != null ? template.getDiscount() : 0);
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "message", "success",
            "data", data
        ));
    }

    /**
     * SecurityContext에서 인증된 사용자의 customerIdx를 반환
     * JwtFilter에서 이미 JWT를 검증하고 SecurityContext에 저장함
     * @param request HTTP 요청 (현재는 사용하지 않지만 일관성을 위해 유지)
     * @return customerIdx (사용자 고유 ID)
     */
    private Integer getCustomerIdxFromToken(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("🔍 [MypageController] Authentication 객체: " + authentication);
            
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                System.out.println("🔍 [MypageController] Principal 타입: " + principal.getClass().getName());
                System.out.println("🔍 [MypageController] Principal 값: " + principal);
                
                if (principal instanceof CustomerAdminSignupDTO) {
                    CustomerAdminSignupDTO dto = (CustomerAdminSignupDTO) principal;
                    System.out.println("✅ [MypageController] customerIdx 추출 성공: " + dto.getCustomerIdx());
                    return dto.getCustomerIdx();
                } else {
                    System.out.println("❌ [MypageController] Principal이 CustomerAdminSignupDTO가 아닙니다.");
                }
            } else {
                System.out.println("❌ [MypageController] Authentication이 null입니다.");
            }
            
            return null;
            
        } catch (Exception e) {
            System.out.println("❌ [MypageController] 인증 정보 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

