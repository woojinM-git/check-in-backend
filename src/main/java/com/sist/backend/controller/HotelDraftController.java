package com.sist.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.hotel.HotelDraftDto;
import com.sist.backend.service.HotelDraftService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotel")
@RequiredArgsConstructor
@Tag(name = "호텔 임시저장", description = "호텔 등록 임시저장 관련 API")
public class HotelDraftController {
    
    private final HotelDraftService hotelDraftService;
    
    // 임시저장 데이터 저장
    @PostMapping("/draft")
    @Operation(summary = "임시저장", description = "호텔 등록 폼 데이터를 임시저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "임시저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> saveDraft(
            @RequestBody Map<String, Object> requestBody) {
        try {
            // 실제 구현 시에는 인증된 사용자의 adminIdx를 가져와야 함
            Integer adminIdx = 1; // 임시로 1 사용
            
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
        @ApiResponse(responseCode = "404", description = "임시저장 데이터 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getDraft() {
        try {
            // 실제 구현 시에는 인증된 사용자의 adminIdx를 가져와야 함
            Integer adminIdx = 1; // 임시로 1 사용
            
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
    
    // 임시저장 데이터 삭제
    @DeleteMapping("/draft")
    @Operation(summary = "임시저장 삭제", description = "임시저장 데이터를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> deleteDraft() {
        try {
            // 실제 구현 시에는 인증된 사용자의 adminIdx를 가져와야 함
            Integer adminIdx = 1; // 임시로 1 사용
            
            hotelDraftService.deleteDraft(adminIdx);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "임시저장 데이터가 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "임시저장 삭제 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 임시저장 상태 확인
    @GetMapping("/draft/status")
    @Operation(summary = "임시저장 상태 확인", description = "임시저장 데이터 존재 여부를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상태 확인 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getDraftStatus() {
        try {
            // 실제 구현 시에는 인증된 사용자의 adminIdx를 가져와야 함
            Integer adminIdx = 1; // 임시로 1 사용
            
            boolean hasDraft = hotelDraftService.hasDraft(adminIdx);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasDraft", hasDraft);
            response.put("message", hasDraft ? "임시저장 데이터가 있습니다." : "임시저장 데이터가 없습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "임시저장 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
