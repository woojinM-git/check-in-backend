package com.sist.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.hotel.HotelDraftDto;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.HotelDraftService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotel")
@RequiredArgsConstructor
@Tag(name = "호텔 임시저장", description = "호텔 등록 임시저장 관련 API")
public class HotelDraftController {
    
    private final HotelDraftService hotelDraftService;
    private final JwtProvider jwtProvider;
    
    // 임시저장 데이터 저장
    @PostMapping("/draft")
    @Operation(summary = "임시저장", description = "호텔 등록 폼 데이터를 임시저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "임시저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> saveDraft(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        try {
            // JWT에서 adminIdx 추출
            Integer adminIdx = getAdminIdxFromToken(request);
            if (adminIdx == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "인증 정보가 유효하지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            String formData = (String) requestBody.get("formData");
            String lastTab = (String) requestBody.get("lastTab");
            Integer progress = (Integer) requestBody.get("progress");
            
            HotelDraftDto savedDraft = hotelDraftService.saveDraft(adminIdx, formData, lastTab, progress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "임시저장이 완료되었습니다.");
            response.put("data", savedDraft);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "임시저장 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 임시저장 데이터 조회
    @GetMapping("/draft")
    @Operation(summary = "임시저장 조회", description = "저장된 임시저장 데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "404", description = "임시저장 데이터 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getDraft(HttpServletRequest request) {
        try {
            // JWT에서 adminIdx 추출
            Integer adminIdx = getAdminIdxFromToken(request);
            if (adminIdx == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "인증 정보가 유효하지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            Optional<HotelDraftDto> draft = hotelDraftService.getDraft(adminIdx);
            
            Map<String, Object> response = new HashMap<>();
            if (draft.isPresent()) {
                response.put("success", true);
                response.put("message", "임시저장 데이터를 조회했습니다.");
                response.put("data", draft.get());
            } else {
                response.put("success", false);
                response.put("message", "임시저장 데이터가 없습니다.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "임시저장 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * HTTP 요청의 쿠키에서 JWT 토큰을 추출하고 adminIdx를 반환
     * @param request HTTP 요청
     * @return adminIdx (관리자 고유 ID)
     */
    private Integer getAdminIdxFromToken(HttpServletRequest request) {
        try {
            // 1. 쿠키에서 accessToken 가져오기
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return null;
            }

            String accessToken = null;
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }

            if (accessToken == null) {
                return null;
            }

            // 2. JWT 토큰 검증
            if (!jwtProvider.verify(accessToken)) {
                return null;
            }

            // 3. JWT에서 adminIdx 추출
            Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            Object adminIdxObj = claims.get("adminIdx");
            
            if (adminIdxObj == null) {
                return null;
            }
            
            // 4. adminIdx 반환
            if (adminIdxObj instanceof Integer) {
                return (Integer) adminIdxObj;
            } else if (adminIdxObj instanceof String) {
                try {
                    return Integer.parseInt((String) adminIdxObj);
                } catch (NumberFormatException e) {
                    System.err.println("adminIdx 파싱 오류: " + e.getMessage());
                    return null;
                }
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
