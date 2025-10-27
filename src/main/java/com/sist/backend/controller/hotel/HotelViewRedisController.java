package com.sist.backend.controller.hotel;

import com.sist.backend.dto.hotel.HotelViewRedisResponse;
import com.sist.backend.service.hotel.HotelViewRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
@Tag(name="호텔 조회수 API", description = "Redis 기반 실시간 접속자 관리")
public class HotelViewRedisController {

    private final HotelViewRedisService hotelViewRedisService;

    /**
     * 호텔 상세 진입 시 세션 등록 (POST)
     * 
     * - 프론트엔드가 페이지 진입 시 1회 호출
     * - sessionStorage 기반 sessionId를 받아서 Redis에 등록
     * - TTL 3분, 중복 등록 방지
     */
    @Operation(
            summary = "호텔 상세 진입 (조회자 등록)",
            description = "호텔 상세 페이지에 진입할 때 Redis에 세션을 등록하고, 현재 활성 접속자 수를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 및 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 본문 누락 또는 잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @Parameters({
            @Parameter(name = "contentId", description = "호텔 고유 ID", example = "142769"),
            @Parameter(name = "sessionId", description = "프론트엔드 세션 ID", example = "abc123-uuid")
    })
    @PostMapping("/{contentId}/views")
    public ResponseEntity<HotelViewRedisResponse> enterHotelDetail(
            @PathVariable String contentId,
            @RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("[뷰카운트] sessionId 누락 - contentId = {}", contentId);
            return ResponseEntity.badRequest().build();
        }

        hotelViewRedisService.addActiveViewer(contentId, sessionId);
        int count = hotelViewRedisService.getActiveViewerCount(contentId);
        log.debug("[뷰카운트] 세션 등록 - contentId = {}, sessionId = {}, 현재 인원 = {}", 
                  contentId, sessionId, count);
        
        return ResponseEntity.ok(HotelViewRedisResponse.of(count));
    }

    // 현재 실시간 조회자 수 조회 (선택적으로 TTL 갱신)
    @Operation(summary = "호텔 실시간 조회수 조회",
            description = "호텔 상세 페이지의 현재 접속자 수를 조회합니다. sessionId가 있으면 TTL 갱신도 수행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 호텔 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @Parameters({
            @Parameter(name = "sessionId", description = "프론트엔드 세션 ID (선택)", example = "abc123-uuid")
    })
    @GetMapping("/{contentId}/views")
    public ResponseEntity<HotelViewRedisResponse> getViews(
            @PathVariable String contentId,
            @RequestParam(required = false) String sessionId) {
        
        // sessionId가 제공되면 TTL 갱신
        if (sessionId != null && !sessionId.isEmpty()) {
            hotelViewRedisService.refreshViewerTTL(contentId, sessionId);
            log.debug("[뷰카운트] TTL 갱신 - contentId = {}, sessionId = {}", contentId, sessionId);
        }

        // 현재 접속자 수 조회
        int count = hotelViewRedisService.getActiveViewerCount(contentId);
        log.debug("[뷰카운트] 조회 - contentId = {}, 활성 인원 수 = {}", contentId, count);

        return ResponseEntity.ok(HotelViewRedisResponse.of(count));
    }

    // 호텔 이탈시 세션 제거
    @Operation(summary = "호텔 상세 이탈", description = "호텔 상세 페이지를 벗어날 때 Redis에서 세션을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제거 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{contentId}/views")
    public ResponseEntity<HotelViewRedisResponse> leaveHotelDetail(
            @PathVariable String contentId,
            @RequestBody Map<String, String> body)
    {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("[뷰카운트] sessionId 누락 - contentId = {}", contentId);
            return ResponseEntity.badRequest().build();
        }

        hotelViewRedisService.removeViewer(contentId, sessionId);
        log.debug("[뷰카운트] 세션 제거 - contentId = {}, sessionId = {}", contentId, sessionId);

        return ResponseEntity.ok(HotelViewRedisResponse.success());
    }

}
