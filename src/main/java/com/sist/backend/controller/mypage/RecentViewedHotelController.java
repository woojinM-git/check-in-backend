package com.sist.backend.controller.mypage;

import com.sist.backend.dto.mypage.RecentViewedHotelRequest;
import com.sist.backend.dto.mypage.RecentViewedHotelResponse;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.service.mypage.RecentViewedHotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recentHotels")
@Tag(name = "최근 본 호텔", description = "최근 본 호텔 관리 API")
public class RecentViewedHotelController {

    private final RecentViewedHotelService recentViewedHotelService;

    @PostMapping
    @Operation(summary = "최근 본 호텔 기록 저장", description = "최근에 조회한 호텔을 기록합니다.")
    public ResponseEntity<?> recordRecentHotel(
        @Valid @RequestBody RecentViewedHotelRequest request,
        HttpServletRequest httpRequest
    ) {
        Integer customerIdx = getCustomerIdx();
        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 정보가 유효하지 않습니다."));
        }

        recentViewedHotelService.recordHotelView(customerIdx, request.getContentId());
        return ResponseEntity.ok(Map.of("message", "success"));
    }

    @GetMapping
    @Operation(summary = "최근 본 호텔 목록 조회", description = "최근에 본 호텔 목록을 최신순으로 조회합니다.")
    public ResponseEntity<?> getRecentHotels(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        HttpServletRequest httpRequest
    ) {
        Integer customerIdx = getCustomerIdx();
        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 정보가 유효하지 않습니다."));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RecentViewedHotelResponse> result = recentViewedHotelService.getRecentViewedHotels(customerIdx, pageable);

        return ResponseEntity.ok(Map.of(
            "message", "success",
            "data", result.getContent(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        ));
    }

    @GetMapping("/count")
    @Operation(summary = "최근 본 호텔 개수 조회", description = "최근 본 호텔 기록 개수를 조회합니다.")
    public ResponseEntity<?> getRecentHotelCount(HttpServletRequest httpRequest) {
        Integer customerIdx = getCustomerIdx();
        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 정보가 유효하지 않습니다."));
        }

        long count = recentViewedHotelService.countRecentViewedHotels(customerIdx);
        return ResponseEntity.ok(Map.of(
            "message", "success",
            "count", count
        ));
    }

    @DeleteMapping("/{recentViewedIdx}")
    @Operation(summary = "최근 본 호텔 개별 삭제", description = "특정 최근 본 호텔 기록을 삭제합니다.")
    public ResponseEntity<?> deleteRecentHotel(
        @PathVariable Integer recentViewedIdx,
        HttpServletRequest httpRequest
    ) {
        Integer customerIdx = getCustomerIdx();
        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 정보가 유효하지 않습니다."));
        }

        recentViewedHotelService.deleteRecentViewedHotel(customerIdx, recentViewedIdx);
        return ResponseEntity.ok(Map.of("message", "success"));
    }

    @DeleteMapping
    @Operation(summary = "최근 본 호텔 전체 삭제", description = "최근 본 호텔 기록을 전체 삭제합니다.")
    public ResponseEntity<?> deleteAllRecentHotels(HttpServletRequest httpRequest) {
        Integer customerIdx = getCustomerIdx();
        if (customerIdx == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 정보가 유효하지 않습니다."));
        }

        recentViewedHotelService.deleteAllRecentViewedHotels(customerIdx);
        return ResponseEntity.ok(Map.of("message", "success"));
    }

    private Integer getCustomerIdx() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomerAdminSignupDTO dto) {
                return dto.getCustomerIdx();
            }
            log.warn("인증 Principal이 CustomerAdminSignupDTO가 아닙니다. principal={}", principal);
            return null;
        } catch (Exception e) {
            log.error("인증 정보 처리 중 오류 발생", e);
            return null;
        }
    }
}

