package com.sist.backend.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.SearchRequestDto;
import com.sist.backend.entity.Center;
import com.sist.backend.service.CenterService;

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
    
    /**
     * 고객센터 글 등록
     */
    @PostMapping("/posts")
    @Operation(summary = "고객센터 글 등록", description = "새로운 고객센터 글을 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "글 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Center> createCenter(@RequestBody Center center) {
        try {
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
            @PathVariable Integer centerIdx) {
        
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
            @PathVariable Integer centerIdx,
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
            @PathVariable Integer centerIdx) {
        
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
        
        Pageable pageable = Pageable.ofSize(searchRequest.getSize()).withPage(searchRequest.getPage());
        Page<Center> centerPage = centerService.searchByMultipleConditions(
            searchRequest.getMainCategory(), 
            searchRequest.getSubCategory(), 
            searchRequest.getStatus(), 
            searchRequest.getPriority(), 
            searchRequest.getCustomerIdx(), 
            searchRequest.getAdminIdx(), 
            searchRequest.getTitle(),
            pageable
        );
        return ResponseEntity.ok(centerPage);
    }
    
}
