package com.sist.backend.controller.mypage;

import com.sist.backend.entity.Review;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.ReviewService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtProvider jwtProvider;

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
            
            List<Review> reviews = reviewService.getMyReviews(customerIdx);
            
            return ResponseEntity.ok(Map.of("reviews", reviews));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()));
        }
    }

    /**
     * HTTP 요청의 Authorization 헤더 또는 쿠키에서 JWT 토큰을 추출하고 사용자 ID를 반환
     */
    private Integer getCustomerIdxFromToken(HttpServletRequest request) {
        try {
            String accessToken = null;
            
            // 1. Authorization 헤더에서 Bearer 토큰 확인
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
            
            // 2. 쿠키에서 확인
            if (accessToken == null) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("accessToken".equals(cookie.getName())) {
                            accessToken = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            if (accessToken == null) {
                return null;
            }

            // 3. JWT 토큰 검증
            if (!jwtProvider.verify(accessToken)) {
                return null;
            }

            // 4. JWT에서 customerIdx 추출
            Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            Object customerIdxObj = claims.get("customerIdx");
            Integer customerIdx = null;
            
            if (customerIdxObj != null) {
                if (customerIdxObj instanceof Integer) {
                    customerIdx = (Integer) customerIdxObj;
                } else if (customerIdxObj instanceof String) {
                    try {
                        customerIdx = Integer.parseInt((String) customerIdxObj);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }

            return customerIdx;

        } catch (Exception e) {
            return null;
        }
    }
}

