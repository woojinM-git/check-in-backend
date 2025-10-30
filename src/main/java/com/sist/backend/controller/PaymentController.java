package com.sist.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.dto.PaymentResponseDto;
import com.sist.backend.service.MailService;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.entity.Customer;
import com.sist.backend.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "결제", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final MailService mailService;
    private final CustomerRepository customerRepository;

    @PostMapping("/confirm")
    @Operation(summary = "결제 확인", description = "토스페이먼츠 결제를 확인하고 데이터베이스에 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 확인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PaymentResponseDto> confirmPayment(@RequestBody PaymentRequestDto request) {
        try {
            // 1단계: 결제 검증 및 DB 저장 (트랜잭션으로 보호)
            PaymentResponseDto response = paymentService.verifyAndSavePayment(request);

            // 2단계: 결제 완료 후 처리 (이메일 발송 - 비동기, 실패해도 롤백 안됨)
            // 중요: 이미 처리된 결제는 이메일을 재발송하지 않음
            if (response.getSuccess()) {
                sendEmailAsync(request, response.getQrUrl());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("결제 확인 실패: orderId={}", request.getOrderId(), e);
            return ResponseEntity.badRequest()
                    .body(PaymentResponseDto.builder()
                            .success(false)
                            .message("결제 확인 실패: " + e.getMessage())
                            .build());
        }
    }

    @Async
    protected void sendEmailAsync(PaymentRequestDto request, String qrUrl) {
        try {
            // 고객 이메일/이름/전화가 비어있으면 DB에서 보강
            if ((request.getCustomerEmail() == null || request.getCustomerEmail().isEmpty())
                    && request.getCustomerIdx() != null) {
                customerRepository.findById(request.getCustomerIdx()).ifPresent((Customer c) -> {
                    if (request.getCustomerEmail() == null) {
                        request.setCustomerEmail(c.getEmail());
                    }
                    if (request.getCustomerName() == null) {
                        request.setCustomerName(c.getName());
                    }
                    if (request.getCustomerPhone() == null) {
                        request.setCustomerPhone(c.getPhone());
                    }
                });
            }
            boolean emailSent = false;
            if ("hotel_reservation".equals(request.getType())) {
                emailSent = mailService.sendHotelReservationEmail(request, qrUrl);
            } else if ("used_hotel".equals(request.getType())) {
                emailSent = mailService.sendUsedHotelPurchaseEmail(request, qrUrl);
            }
            log.info("이메일 발송 완료: orderId={}, emailSent={}", request.getOrderId(), emailSent);
        } catch (Exception e) {
            log.error("이메일 발송 실패 (결제는 이미 완료됨): orderId={}", request.getOrderId(), e);
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
