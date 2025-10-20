package com.sist.backend.controller.hotel;

import com.sist.backend.dto.hotel.HotelViewRedisResponse;
import com.sist.backend.service.hotel.HotelViewRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 호텔 실시간 조회수 컨트롤러
 *
 * 역할:
 * 프론트엔드 요청 수신
 * RedisService를 호출해 로직 수행
 * 결과를 HotelViewResponse DTO로 반환
 */
@Slf4j
@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name="Hotel view", description = "호텔 실시간 조회수 API")
public class HotelViewRedisController {

    private final HotelViewRedisService hotelViewRedisService;

    /**
     * 호텔 상세 진입 시 호출
     * 세션 기준으로 Redis에 등록 (TTL 1분)
     * contentId는 DB에서는 VARCHAR(50)이지만, URI에서는 숫자처럼 들어올 수도 있음
     */
    @Operation(summary = "호텔 상세 진입",description = "호텔 상세 페이지 진입시 활성 사용자로 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (contentId 누락 등)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/{contentId}/view")
    public ResponseEntity<?> enterHotelDetail(
            @PathVariable String contentId, //DB는 varchar(50)임
            HttpServletRequest request
    ){
        String sessionId = request.getSession().getId();
        hotelViewRedisService.addActiveViewer(contentId,sessionId);
        log.info("호텔{} 상세 페이지 진입 -세션: {}",contentId,sessionId);
        return ResponseEntity.ok(HotelViewRedisResponse.success());
    }

    // 현재 실시간 조회자 수 조회
    @Operation(summary = "현재 조회자 수 조회", description = "현재 호텔 상세 페이지를 보고 있는 인원 수를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 호텔 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/views")
    public ResponseEntity<?> getActiveViewCount(@PathVariable String contentId) {
        int count = hotelViewRedisService.getActiveViewerCount(contentId);
        return ResponseEntity.ok(HotelViewRedisResponse.of(count));
    }

    //호탤 이탈시 세션 제거
    @Operation(summary = "호텔 상세 이탈", description = "페이지를 벗어날 때 Redis에서 세션을 제거합니다.")
    @PostMapping("/{contentId}/leave")
    public ResponseEntity<?> leaveHotelDetail(
            @PathVariable String contentId,
            HttpServletRequest request
    ) {
        String sessionId = request.getSession().getId();
        hotelViewRedisService.removeViewer(contentId, sessionId);
        return ResponseEntity.ok(HotelViewRedisResponse.success());
    }

}
