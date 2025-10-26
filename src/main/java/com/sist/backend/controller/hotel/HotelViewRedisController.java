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
@Tag(name="호텔 조회수 API", description = "Redis 기반 실시간 접속자 관리")
public class HotelViewRedisController {

    private final HotelViewRedisService hotelViewRedisService;

    /**
     * 호텔 상세 진입 시 호출
     * contentId는 DB에서는 VARCHAR(50)이지만, URI에서는 숫자처럼 들어올 수도 있음
     *  *****리팩터링 예정 사항*****
     * /view 제거 views(Get)방식 하나로 등록 및 조회를 동시 수행예정
     * leave는 유지하고 프론트가 페이지 이탈 감지시 호출 가능
     * TTL은 1분에서 3분으로 확장예정( 유저가 머무는 시간 예측)
     */


    // 현재 실시간 조회자 수 조회
    @Operation(summary = "호텔 접속자 등록 및 조회",
            description = "호텔 상세 페이지 진입 시 활성 사용자 등록 및 현재 접속자 수를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 호텔 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/views")
    public ResponseEntity<HotelViewRedisResponse> registerAndGetViews(
            @PathVariable String contentId,
            HttpServletRequest request) {
        //요청이 들어왔을때 호텔번호 세션아이디값 활성자 수를 반환
        String sessionId = request.getSession(true).getId();
        hotelViewRedisService.addActiveViewer(contentId,sessionId);

        int count = hotelViewRedisService.getActiveViewerCount(contentId);
        log.debug("[뷰카운트] contentId = {}, sessionId = {}, acivateCount(활성화된 인원수) = {activateCount}",contentId,sessionId,count);

        return ResponseEntity.ok(HotelViewRedisResponse.of(count));
    }

    //호탤 이탈시 세션 제거
    //프론트에서 선택적 사용 예정 :TTL만료되기전 정확한 인원 반영가능
    @Operation(summary = "호텔 상세 이탈", description = "호텔 상세 페이지를 벗어날 때 Redis에서 세션을 제거합니다.")
    @DeleteMapping("/{contentId}/views")
    public ResponseEntity<HotelViewRedisResponse> leaveHotelDetail(
            @PathVariable String contentId,
            HttpServletRequest request)
    {
        String sessionId = request.getSession().getId();
        hotelViewRedisService.removeViewer(contentId, sessionId);
        log.debug("[뷰카운트] contentId = {} 세션 종료 ={}",contentId,sessionId);

        return ResponseEntity.ok(HotelViewRedisResponse.success());
    }

}
