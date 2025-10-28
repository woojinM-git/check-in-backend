package com.sist.backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sist.backend.dto.hotel.HotelDraftDto;
import com.sist.backend.entity.RegistrationRequest;
import com.sist.backend.util.JwtUtils;
import com.sist.backend.repository.RegistrationRequestRepository;
import com.sist.backend.service.HotelDraftService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotel")
@RequiredArgsConstructor
@Tag(name = "호텔 임시저장", description = "호텔 등록 임시저장 관련 API")
public class HotelDraftController {
    
    private final HotelDraftService hotelDraftService;
    private final JwtUtils jwtUtils;
    private final RegistrationRequestRepository registrationRequestRepository;
    
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
            Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
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
            Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
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

   @PostMapping("/register")
   @Operation(summary = "호텔 등록 요청", description = "임시저장된 호텔 정보를 등록 요청으로 제출합니다.")
   @ApiResponses(value = {
           @ApiResponse(responseCode = "200", description = "등록 요청 성공"),
           @ApiResponse(responseCode = "401", description = "인증되지 않음"),
           @ApiResponse(responseCode = "400", description = "잘못된 요청"),
           @ApiResponse(responseCode = "500", description = "서버 오류")
   })
   public ResponseEntity<Map<String, Object>> registerHotel(
           @Parameter(description = "입력되어있는 정보들이 담긴 드래프트 Idx", example = "1")
           @RequestParam("draftIdx") Integer draftIdx,
           HttpServletRequest request) {
       
       try {
           // JWT에서 adminIdx 추출
           Integer adminIdx = jwtUtils.getAdminIdxFromRequest(request);
           if (adminIdx == null) {
               Map<String, Object> response = new HashMap<>();
               response.put("success", false);
               response.put("message", "인증 정보가 유효하지 않습니다.");
               return ResponseEntity.status(401).body(response);
           }
           
           // RegistrationRequest 객체 생성 및 설정
           RegistrationRequest registrationRequest = new RegistrationRequest();
           registrationRequest.setDraftIdx(draftIdx);
           registrationRequest.setAdminIdx(adminIdx);
           registrationRequest.setRegiDate(LocalDateTime.now());
           registrationRequest.setStatus(0);
           
           // Repository를 통해 저장
           RegistrationRequest savedRequest = registrationRequestRepository.save(registrationRequest);
           
           Map<String, Object> response = new HashMap<>();
           response.put("success", true);
           response.put("message", "등록 요청이 완료되었습니다.");
           response.put("registrationIdx", savedRequest.getRegistrationIdx());
           
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           Map<String, Object> response = new HashMap<>();
           response.put("success", false);
           response.put("message", "등록 요청 중 오류가 발생했습니다: " + e.getMessage());

           return ResponseEntity.badRequest().body(response);
       }
   }

}
