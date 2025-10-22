package com.sist.backend.service;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.dto.PaymentResponseDto;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.util.QRCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RoomPaymentRepository roomPaymentRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final TossPaymentsService tossPaymentsService;
    private final MailService mailService;
    private final QRCodeGenerator qrCodeGenerator;

    @Transactional
    public PaymentResponseDto verifyAndSavePayment(PaymentRequestDto request) {
        boolean emailSent = false;
        try {
            log.info("결제 검증 시작: orderId={}, amount={}", request.getOrderId(), request.getAmount());

            // TODO: 결제 금액 검증 로직 추가 (최소/최대 금액 체크)
            // TODO: 중복 결제 방지 로직 추가 (같은 orderId로 이미 결제된 경우 체크)
            // 1. TossPayments API로 결제 검증
            Map<String, Object> tossResponse = tossPaymentsService.confirmPayment(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount()
            );

            // 2. 검증 결과 확인
            String status = (String) tossResponse.get("status");
            if (!"DONE".equals(status)) {
                throw new RuntimeException("TossPayments 결제 검증 실패: status=" + status);
            }

            // 3. 결제 정보 저장
            RoomPayment roomPayment = RoomPayment.builder()
                    .customerIdx(request.getCustomerIdx())
                    .couponIdx(0) // TODO: 실제 쿠폰 시스템 연동 필요
                    .price(request.getAmount())
                    .status(1) // 결제 완료
                    .promotionPayIdx(0) // TODO: 프로모션 시스템 연동 필요
                    .paymentKey(request.getPaymentKey())
                    .pointsUsed(request.getPointsUsed() != null ? request.getPointsUsed() : 0)
                    .method(request.getMethod() != null ? request.getMethod() : "card")
                    .receiptUrl("https://api.tosspayments.com/v1/payments/" + request.getPaymentKey() + "/receipt")
                    .approvedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            RoomPayment savedPayment = roomPaymentRepository.save(roomPayment);

            // 4. 호텔 예약인 경우 예약 정보도 저장
            if ("hotel_reservation".equals(request.getType()) && request.getContentId() != null) {
                saveRoomReservation(request, savedPayment.getOrderIdx());
            }

            // 5. QR 코드 생성
            String qrUrl = qrCodeGenerator.generateQRCodeUrl(request.getOrderId());

            // 6. 이메일 발송
            try {
                if ("hotel_reservation".equals(request.getType())) {
                    emailSent = mailService.sendHotelReservationEmail(request, qrUrl);
                } else if ("used_hotel".equals(request.getType())) {
                    emailSent = mailService.sendUsedHotelPurchaseEmail(request, qrUrl);
                }
                // TODO: 이메일 발송 실패 시 재시도 로직 추가
                // TODO: 이메일 발송 상태 추적 시스템 구축
            } catch (Exception e) {
                log.error("이메일 발송 실패: orderId={}", request.getOrderId(), e);
                emailSent = false;
            }

            log.info("결제 정보 저장 완료: orderIdx={}, emailSent={}", savedPayment.getOrderIdx(), emailSent);

            return PaymentResponseDto.builder()
                    .success(true)
                    .message("결제가 성공적으로 완료되었습니다.")
                    .orderId(request.getOrderId())
                    .paymentKey(request.getPaymentKey())
                    .amount(request.getAmount())
                    .status("DONE")
                    .approvedAt(savedPayment.getApprovedAt())
                    .receiptUrl(savedPayment.getReceiptUrl())
                    .qrUrl(qrUrl)
                    .emailSent(emailSent)
                    .build();

        } catch (Exception e) {
            log.error("결제 검증 및 저장 실패", e);
            return PaymentResponseDto.builder()
                    .success(false)
                    .message("결제 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .orderId(request.getOrderId())
                    .emailSent(emailSent)
                    .build();
        }
    }

    private void saveRoomReservation(PaymentRequestDto request, Integer orderIdx) {
        try {
            // TODO: 예약 가능 여부 사전 체크 로직 추가
            // TODO: 객실 재고 확인 및 차감 로직 추가
            // TODO: 체크인/체크아웃 날짜 유효성 검증 강화

            RoomReservation reservation = RoomReservation.builder()
                    .customerIdx(request.getCustomerIdx()) // TODO: 실제 로그인된 사용자 ID 사용
                    .roomIdx(request.getRoomId())
                    .contentid(request.getContentId()) // contentId는 String 타입
                    .orderIdx(orderIdx)
                    .checkinDate(request.getCheckIn() != null ? LocalDate.parse(request.getCheckIn()) : null)
                    .checkoutDate(request.getCheckOut() != null ? LocalDate.parse(request.getCheckOut()) : null)
                    .guest(request.getGuests())
                    .totalPrice(request.getTotalPrice())
                    .status(1) // 예약 확정
                    .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            roomReservationRepository.save(reservation);
            log.info("예약 정보 저장 완료: reservIdx={}", reservation.getReservIdx());
        } catch (Exception e) {
            log.error("예약 정보 저장 실패", e);
            throw new RuntimeException("예약 정보 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public PaymentResponseDto getPaymentInfo(String orderId) {
        try {
            // 주문 ID로 결제 정보 조회 (실제 구현에서는 orderId로 조회)
            // 현재는 간단히 성공 응답만 반환
            return PaymentResponseDto.builder()
                    .success(true)
                    .message("결제 정보 조회 성공")
                    .orderId(orderId)
                    .status("DONE")
                    .build();
        } catch (Exception e) {
            log.error("결제 정보 조회 실패", e);
            throw new RuntimeException("결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

}
