package com.sist.backend.controller.mypage;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Review;
import com.sist.backend.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성
     */
    @PostMapping("")
    public ResponseEntity<?> createReview(
            @RequestBody Map<String, Object> request,
            HttpServletRequest req) {
        try {
            // JWT에서 사용자 정보 가져오기
            Integer customerIdx = getCustomerIdxFromToken(req);
            
            if (customerIdx == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증 정보가 유효하지 않습니다."));
            }
            
            Integer reservationIdx = (Integer) request.get("reservationIdx");
            Integer rating = (Integer) request.get("rating");
            String content = (String) request.get("content");
            
            // 리뷰 작성
            Review review = reviewService.createReview(reservationIdx, customerIdx, rating, content);
            
            return ResponseEntity.ok(Map.of(
                "message", "리뷰가 등록되었습니다.",
                "reviewIdx", review.getReviewIdx(),
                "points", 1000
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()));
        }
    }

    /**
     * 리뷰 작성 여부 확인
     */
    @GetMapping("/check/{reservationId}")
    public ResponseEntity<?> checkReviewExists(
            @PathVariable Integer reservationId,
            HttpServletRequest req) {
        try {
            Integer customerIdx = getCustomerIdxFromToken(req);
            
            if (customerIdx == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증 정보가 유효하지 않습니다."));
            }
            
            boolean hasReview = reviewService.hasReview(reservationId, customerIdx);
            
            return ResponseEntity.ok(Map.of(
                "hasReview", hasReview,
                "reservationId", reservationId
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()));
        }
    }

    /**
     * 내가 작성한 리뷰 조회
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<?> getMyReviews(HttpServletRequest req) {
        try {
            Integer customerIdx = getCustomerIdxFromToken(req);
            
            if (customerIdx == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증 정보가 유효하지 않습니다."));
            }
            
            List<com.sist.backend.dto.mypage.WrittenReviewDTO> reviews = reviewService.getMyReviews(customerIdx);
            
            return ResponseEntity.ok(Map.of("reviews", reviews));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()));
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
            System.out.println("🔍 [ReviewController] Authentication 객체: " + authentication);
            
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                System.out.println("🔍 [ReviewController] Principal 타입: " + principal.getClass().getName());
                
                if (principal instanceof CustomerAdminSignupDTO) {
                    CustomerAdminSignupDTO dto = (CustomerAdminSignupDTO) principal;
                    System.out.println("✅ [ReviewController] customerIdx 추출 성공: " + dto.getCustomerIdx());
                    return dto.getCustomerIdx();
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("❌ [ReviewController] 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

