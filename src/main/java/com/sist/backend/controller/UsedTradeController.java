package com.sist.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.UsedItemDto;
import com.sist.backend.service.UsedTradeService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/used")
@RequiredArgsConstructor
@Tag(name = "양도거래", description = "양도거래 관련 API")
public class UsedTradeController {

    final private UsedTradeService usedTradeService;

    @GetMapping("/list")
    @Operation(summary = "양도거래 목록 조회", description = "페이징 처리된 양도거래 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<UsedItemDto>> getUsedTradeListWithDetails(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "10") 
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(usedTradeService.findAllByStatusOrderByUpdatedAtDescAsDto(pageable));
    }
}
