package com.sist.backend.controller;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.dto.PaymentResponseDto;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "결제", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    @Operation(summary = "결제 확인", description = "토스페이먼츠 결제를 확인하고 데이터베이스에 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 확인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PaymentResponseDto> confirmPayment(@RequestBody PaymentRequestDto request) {
        try {
            PaymentResponseDto response = paymentService.verifyAndSavePayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponseDto.builder()
                            .success(false)
                            .message("결제 확인 실패: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "결제 정보 조회", description = "주문 ID로 결제 정보를 조회합니다.")
    public ResponseEntity<PaymentResponseDto> getPaymentInfo(@PathVariable String orderId) {
        try {
            PaymentResponseDto response = paymentService.getPaymentInfo(orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponseDto.builder()
                            .success(false)
                            .message("결제 정보 조회 실패: " + e.getMessage())
                            .build());
        }
    }
}
