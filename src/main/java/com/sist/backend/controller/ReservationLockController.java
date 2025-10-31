package com.sist.backend.controller;

import com.sist.backend.dto.ReservationLockDto;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.service.ReservationLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 예약 락 컨트롤러
 *
 * 역할: - 호텔 예약 시 중복 예약 방지를 위한 분산 락 API 제공 - 예약하기 버튼 클릭 시 락 생성 - 결제 완료 또는 페이지 이탈
 * 시 락 해제
 */
@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "예약 락", description = "호텔 예약 중복 방지 락 관리 API")
public class ReservationLockController {

    private final ReservationLockService reservationLockService;

    /**
     * 예약 락 생성 (예약하기 버튼 클릭 시)
     *
     * @param request 락 생성 요청 (contentId, roomId)
     * @return 락 생성 결과
     */
    @PostMapping("/lock")
    @Operation(
            summary = "예약 락 생성",
            description = "호텔 예약하기 버튼 클릭 시 해당 객실에 대한 예약 락을 생성합니다. TTL 10분."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "락 생성 성공"),
        @ApiResponse(responseCode = "400", description = "이미 락이 존재하거나 잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ReservationLockDto> createLock(@RequestBody ReservationLockDto request) {
        try {
            // SecurityContext에서 고객 정보 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Integer customerIdx = null;

            if (authentication != null && authentication.getPrincipal() instanceof CustomerAdminSignupDTO principal) {
                if ("customer".equalsIgnoreCase(principal.getRole())) {
                    customerIdx = principal.getCustomerIdx();
                }
            }

            // 인증되지 않은 경우
            if (customerIdx == null) {
                log.warn("예약 락 생성 실패: 인증되지 않은 사용자");
                return ResponseEntity.status(401)
                        .body(ReservationLockDto.builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            // 필수 파라미터 검증
            if (request.getContentId() == null || request.getRoomId() == null) {
                log.warn("예약 락 생성 실패: 필수 파라미터 누락 - contentId={}, roomId={}",
                        request.getContentId(), request.getRoomId());
                return ResponseEntity.badRequest()
                        .body(ReservationLockDto.builder()
                                .success(false)
                                .message("호텔 ID와 객실 ID는 필수입니다.")
                                .build());
            }

            // 락 생성
            ReservationLockDto result = reservationLockService.createLock(
                    customerIdx,
                    request.getContentId(),
                    request.getRoomId()
            );

            if (result.getSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("예약 락 생성 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ReservationLockDto.builder()
                            .success(false)
                            .message("예약 락 생성 중 오류가 발생했습니다.")
                            .build());
        }
    }

    /**
     * 예약 락 해제 (페이지 이탈 또는 결제 완료 시)
     *
     * @param request 락 해제 요청 (contentId, roomId)
     * @return 락 해제 결과
     */
    @PostMapping("/unlock")
    @Operation(
            summary = "예약 락 해제",
            description = "결제 완료 또는 페이지 이탈 시 예약 락을 해제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "락 해제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ReservationLockDto> releaseLock(@RequestBody ReservationLockDto request) {
        try {
            // SecurityContext에서 고객 정보 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Integer customerIdx = null;

            if (authentication != null && authentication.getPrincipal() instanceof CustomerAdminSignupDTO principal) {
                if ("customer".equalsIgnoreCase(principal.getRole())) {
                    customerIdx = principal.getCustomerIdx();
                }
            }

            // 인증되지 않은 경우 (beforeunload에서는 인증 실패할 수 있음)
            if (customerIdx == null) {
                log.info("예약 락 해제 시도: 인증 정보 없음 (페이지 이탈로 추정)");
                // 인증 없어도 락 해제는 허용 (단, 소유권 검증은 서비스에서 수행)
                customerIdx = request.getCustomerIdx(); // 클라이언트가 보낸 값 사용
            }

            // 필수 파라미터 검증
            if (request.getContentId() == null || request.getRoomId() == null) {
                log.warn("예약 락 해제 실패: 필수 파라미터 누락");
                return ResponseEntity.badRequest()
                        .body(ReservationLockDto.builder()
                                .success(false)
                                .message("호텔 ID와 객실 ID는 필수입니다.")
                                .build());
            }

            // 락 해제
            ReservationLockDto result = reservationLockService.releaseLock(
                    request.getContentId(),
                    request.getRoomId(),
                    customerIdx
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("예약 락 해제 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ReservationLockDto.builder()
                            .success(false)
                            .message("예약 락 해제 중 오류가 발생했습니다.")
                            .build());
        }
    }

    /**
     * 락 상태 조회 (디버깅용)
     *
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @return 락 정보
     */
    @GetMapping("/lock/status")
    @Operation(
            summary = "락 상태 조회",
            description = "특정 객실의 락 상태를 조회합니다. (디버깅용)"
    )
    public ResponseEntity<?> getLockStatus(
            @Parameter(description = "호텔 ID") @RequestParam String contentId,
            @Parameter(description = "객실 ID") @RequestParam Integer roomId
    ) {
        try {
            boolean isLocked = reservationLockService.isLocked(contentId, roomId);
            Map<String, Object> lockInfo = reservationLockService.getLockInfo(contentId, roomId);

            return ResponseEntity.ok(Map.of(
                    "isLocked", isLocked,
                    "lockInfo", lockInfo != null ? lockInfo : Map.of()
            ));

        } catch (Exception e) {
            log.error("락 상태 조회 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "락 상태 조회 중 오류가 발생했습니다."));
        }
    }
}
