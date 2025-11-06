package com.sist.backend.controller.reservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.reservation.CancelRequestDTO;
import com.sist.backend.dto.reservation.CancelResponseDTO;
import com.sist.backend.service.reservation.ReservationCancelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Cancel", description = "예약 취소/환불 API")
@Slf4j
public class ReservationCancelController {

    private final ReservationCancelService reservationCancelService;

    @PostMapping("/{reservIdx}/cancel")
    @Operation(summary = "예약 취소", description = "Toss 환불 및 예약 상태 갱신")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "success"),
        @ApiResponse(responseCode = "400", description = "invalid request"),
        @ApiResponse(responseCode = "500", description = "server error")
    })
    public ResponseEntity<CancelResponseDTO> cancel(
            @PathVariable Integer reservIdx,
            @RequestBody CancelRequestDTO request) {
        log.info("[CANCEL] API called: reservIdx={} reason={}", reservIdx, request != null ? request.getCancelReason() : null);
        CancelResponseDTO res = reservationCancelService.cancelReservation(reservIdx, request != null ? request.getCancelReason() : null);
        return ResponseEntity.ok(res);
    }
}
