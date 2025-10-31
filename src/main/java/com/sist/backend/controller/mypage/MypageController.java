package com.sist.backend.controller.mypage;

import java.util.List;
import java.util.Map;

import com.sist.backend.dto.mypage.ReservationResponseDTO;
import com.sist.backend.dto.mypage.WritableReviewDTO;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.mypage.MyPageService;
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
public class MypageController {

    private final MyPageService myPageService;
    private final CustomerService customerService;

    /* 
     * 마이페이지 예약 내역 조회 API
     * 엔드포인트: GET /api/mypage/reservations?status={status}
     */

    /* 예약 내역 조회 */
    @GetMapping("/reservations")
    public ResponseEntity<?> getReservations(
            @RequestParam(name = "status") String status,
            HttpServletRequest request) {

        // JWT에서 사용자 정보 가져오기
        Integer customerIdx = getCustomerIdxFromToken(request);

        if (customerIdx == null) {
            //인증 정보가 없을 경우 401 에러 반환
            return ResponseEntity.status(401).body(Map.of(
                "message", "인증 정보가 유효하지 않습니다."));
        }

        System.out.println("👤 예약 내역 조회 - customerIdx: " + customerIdx + ", status: " + status);

        // 서비스 호출: 고객 ID와 상태 문자열 전달 (DTO로 변환된 데이터 반환)
        List<ReservationResponseDTO> reservations = myPageService.getMyReservationsByStatus(customerIdx, status);

        // 프론트엔드 mypage/page.js에서 예상하는 JSON 형식 ({"reservations": [...]})에 맞춰 응답
        return ResponseEntity.ok(Map.of("reservations", reservations));
    }

    /* 예약 상세 조회 */
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<?> getReservationDetail(
            @PathVariable Integer reservationId,
            HttpServletRequest request) {
        
        // JWT에서 사용자 정보 가져오기
        Integer customerIdx = getCustomerIdxFromToken(request);

        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of(
                "message", "인증 정보가 유효하지 않습니다."));
        }

        try {
            System.out.println("👤 예약 상세 조회 - customerIdx: " + customerIdx + ", reservationId: " + reservationId);
            
            // 서비스 호출: 예약 상세 정보 조회
            ReservationResponseDTO reservation = myPageService.getReservationDetail(reservationId, customerIdx);
            
            if (reservation == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "message", "예약 정보를 찾을 수 없습니다."));
            }
            
            return ResponseEntity.ok(reservation);
            
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

