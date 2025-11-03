package com.sist.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Coupon;
import com.sist.backend.repository.CouponRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "쿠폰", description = "쿠폰 조회/관리 API")
public class CouponController {

    private final CouponRepository couponRepository;

    @GetMapping("/my")
    @Operation(summary = "내 쿠폰 목록", description = "로그인한 고객의 사용 가능한 쿠폰 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<Map<String, Object>> getMyCoupons() {
        Integer customerIdx = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomerAdminSignupDTO principal) {
            if ("customer".equalsIgnoreCase(principal.getRole())) {
                customerIdx = principal.getCustomerIdx();
            }
        }

        if (customerIdx == null) {
            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "data", List.of()
            ));
        }

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> all = couponRepository.findByCustomerIdx(customerIdx);
        List<Map<String, Object>> result = all.stream()
                .filter(c -> c.getStatus() == null || !c.getStatus()) // status=false(미사용)
                .filter(c -> c.getEndDate() == null || !c.getEndDate().isBefore(now)) // 만료 제외
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("couponIdx", c.getCouponIdx());
                    m.put("templateIdx", c.getTemplateIdx());
                    m.put("customerIdx", c.getCustomerIdx());
                    m.put("adminIdx", c.getAdminIdx());
                    m.put("createDate", c.getCreateDate());
                    m.put("endDate", c.getEndDate());
                    m.put("status", c.getStatus() != null && c.getStatus() ? 1 : 0);
                    m.put("templateName", c.getCouponTemplate() != null ? c.getCouponTemplate().getTemplateName() : "");
                    m.put("discount", c.getCouponTemplate() != null ? c.getCouponTemplate().getDiscount() : 0);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "message", "success",
                "data", result
        ));
    }
}
