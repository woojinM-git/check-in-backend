package com.sist.backend.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.sist.backend.dto.SearchRequestDto;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Center;
import com.sist.backend.service.CenterService;
import com.sist.backend.service.AnswerService;
import com.sist.backend.entity.Answer;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/center")
@RequiredArgsConstructor
@Tag(name = "고객센터", description = "고객센터 게시판 API")
public class CenterController {
    
    private final CenterService centerService;
    private final AnswerService answerService;
    
    /**
     * 고객센터 글 등록
     */
    @PostMapping("/posts")
    @Operation(summary = "고객센터 글 등록", description = "새로운 고객센터 글을 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "글 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "409", description = "이미 신고한 호텔입니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> createCenter(@RequestBody Center center) {
        try {
            // JWT에서 customerIdx 자동 설정 (고객인 경우)
            Integer customerIdx = getCustomerIdxFromToken();
            if (customerIdx != null && center.getCustomerIdx() == null) {
                center.setCustomerIdx(customerIdx);
            }
            
            // 신고인 경우 중복 신고 확인
            if ("신고".equals(center.getMainCategory()) && center.getContentId() != null && customerIdx != null) {
                boolean alreadyReported = centerService.existsReportByContentIdAndCustomerIdx(
                    center.getContentId(), customerIdx
                );
                if (alreadyReported) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "이미 신고한 호텔입니다."));
                }
            }
            
            Center createdCenter = centerService.createCenter(center);
            //createdAt 현재시간으로 설정
            createdCenter.setCreatedAt(LocalDateTime.now());
            createdCenter.setUpdatedAt(LocalDateTime.now());
            createdCenter.setStatus(0);
            createdCenter.setHide(false);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCenter);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    
    /**
     * 문의 답변 조회 (더 구체적인 경로를 먼저 배치)
     */
    @GetMapping("/posts/{centerIdx}/answer")
    @Operation(summary = "문의 답변 조회", description = "특정 문의에 대한 답변을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Answer> getAnswerByCenterIdx(
            @Parameter(description = "문의 고유번호")
            @PathVariable(name = "centerIdx") Integer centerIdx) {
        
        try {
            Answer answer = answerService.getActiveAnswerByCenterIdx(centerIdx)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
            return ResponseEntity.ok(answer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 고객센터 글 상세 조회
     */
    @GetMapping("/posts/{centerIdx}")
    @Operation(summary = "고객센터 글 상세 조회", description = "특정 고객센터 글의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Center> getCenterById(
            @Parameter(description = "고객센터 글 번호")
            @PathVariable(name = "centerIdx") Integer centerIdx) {
        
        try {
            Center center = centerService.getCenterById(centerIdx);
            return ResponseEntity.ok(center);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 고객센터 글 수정
     */
    @PutMapping("/posts/{centerIdx}")
    @Operation(summary = "고객센터 글 수정", description = "기존 고객센터 글을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Center> updateCenter(
            @Parameter(description = "고객센터 글 번호")
            @PathVariable(name = "centerIdx") Integer centerIdx,
            @RequestBody Center center) {
        
        try {
            Center updatedCenter = centerService.updateCenter(centerIdx, center);
            return ResponseEntity.ok(updatedCenter);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 고객센터 글 삭제
     */
    @DeleteMapping("/posts/{centerIdx}")
    @Operation(summary = "고객센터 글 삭제", description = "고객센터 글을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deleteCenter(
            @Parameter(description = "고객센터 글 번호")
            @PathVariable(name = "centerIdx") Integer centerIdx) {
        
        try {
            centerService.deleteCenter(centerIdx);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    
    /**
     * 고객센터 글 검색 (복합 조건) - Request Body 사용
     */
    @PostMapping("/posts/search")
    @Operation(summary = "고객센터 글 검색", description = "다양한 조건으로 고객센터 글을 검색합니다.")
    public ResponseEntity<Page<Center>> searchCenter(@RequestBody SearchRequestDto searchRequest) {
        
        // JWT에서 customerIdx 자동 설정 (고객인 경우, 명시적으로 전달되지 않은 경우)
        Integer customerIdxFromToken = getCustomerIdxFromToken();
        String mainCategory = searchRequest.getMainCategory();
        
        // customerIdx 처리: 문의/신고는 로그인 필수, FAQ는 필터링 없음
        Integer finalCustomerIdx = searchRequest.getCustomerIdx();
        if (mainCategory != null && (mainCategory.equals("문의") || mainCategory.equals("신고"))) {
            // 명시적으로 customerIdx가 전달되지 않은 경우에만 자동 설정
            if (finalCustomerIdx == null) {
                if (customerIdxFromToken == null) {
                    // 로그인하지 않은 경우 빈 결과 반환
                    Pageable pageable = Pageable.ofSize(searchRequest.getSize()).withPage(searchRequest.getPage());
                    return ResponseEntity.ok(Page.empty(pageable));
                }
                // 로그인한 경우 자동으로 customerIdx 설정
                finalCustomerIdx = customerIdxFromToken;
            }
        } else {
            // FAQ 등 다른 카테고리는 customerIdx 필터링 없음 (-1로 설정)
            if (finalCustomerIdx == null) {
                finalCustomerIdx = -1;
            }
        }
        
        // 문의인 경우 contentId 필터링 처리
        String contentIdFilter = null;
        if ("문의".equals(searchRequest.getMainCategory())) {
            if (searchRequest.getContentId() == null) {
                // contentId가 명시되지 않았으면 "NULL" 문자열로 설정하여 IS NULL 조건 적용 (사이트 문의만)
                contentIdFilter = "NULL";
            } else {
                // contentId가 명시되면 해당 호텔 문의만
                contentIdFilter = searchRequest.getContentId();
            }
        } else {
            // 문의가 아닌 경우 contentId 필터링 없음
            contentIdFilter = searchRequest.getContentId();
        }
        
        Pageable pageable = Pageable.ofSize(searchRequest.getSize()).withPage(searchRequest.getPage());
        Page<Center> centerPage = centerService.searchByMultipleConditions(
            searchRequest.getMainCategory(), 
            searchRequest.getSubCategory(), 
            searchRequest.getStatus(), 
            searchRequest.getPriority(), 
            finalCustomerIdx, 
            searchRequest.getAdminIdx(), 
            contentIdFilter,
            searchRequest.getTitle(),
            pageable
        );
        return ResponseEntity.ok(centerPage);
    }
    
    /**
     * 호텔 신고 중복 확인
     */
    @GetMapping("/reports/check/{contentId}")
    @Operation(summary = "호텔 신고 중복 확인", description = "특정 호텔에 대해 이미 신고했는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Map<String, Boolean>> checkReportExists(
            @Parameter(description = "호텔 ID")
            @PathVariable(name = "contentId") String contentId) {
        
        Integer customerIdx = getCustomerIdxFromToken();
        if (customerIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        boolean exists = centerService.existsReportByContentIdAndCustomerIdx(contentId, customerIdx);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
    
    /**
     * SecurityContext에서 인증된 사용자의 customerIdx를 반환
     * JwtFilter에서 이미 JWT를 검증하고 SecurityContext에 저장함
     * @return customerIdx (사용자 고유 ID, 고객이 아닌 경우 null)
     */
    private Integer getCustomerIdxFromToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                
                if (principal instanceof CustomerAdminSignupDTO) {
                    CustomerAdminSignupDTO dto = (CustomerAdminSignupDTO) principal;
                    // 고객인 경우에만 customerIdx 반환
                    if ("customer".equals(dto.getRole())) {
                        return dto.getCustomerIdx();
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
}
