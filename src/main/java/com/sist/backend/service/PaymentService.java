package com.sist.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.dto.PaymentResponseDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.Room;
import com.sist.backend.entity.RoomId;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.util.QRCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RoomPaymentRepository roomPaymentRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final TossPaymentsService tossPaymentsService;
    private final MailService mailService;
    private final QRCodeGenerator qrCodeGenerator;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDto verifyAndSavePayment(PaymentRequestDto request) {
        log.info("결제 검증 시작: orderId={}, amount={}", request.getOrderId(), request.getAmount());

        // 0단계: 이미 처리된 결제인지 확인 (중복 요청 방지)
        Optional<RoomPayment> existingPaymentOpt = roomPaymentRepository.findByPaymentKeyAndStatus(request.getPaymentKey());
        if (existingPaymentOpt.isPresent()) {
            RoomPayment existingPayment = existingPaymentOpt.get();
            log.warn("이미 처리된 결제입니다. 기존 결제 정보를 반환합니다: paymentKey={}, orderIdx={}",
                    request.getPaymentKey(), existingPayment.getOrderIdx());

            return PaymentResponseDto.builder()
                    .success(true)
                    .message("이미 처리된 결제입니다.")
                    .orderId(request.getOrderId())
                    .paymentKey(request.getPaymentKey())
                    .amount(existingPayment.getPrice())
                    .status("DONE")
                    .approvedAt(existingPayment.getApprovedAt())
                    .receiptUrl(existingPayment.getReceiptUrl())
                    .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                    .emailSent(false)
                    .build();
        }

        try {
            // 1단계: TossPayments API로 결제 검증
            Map<String, Object> tossResponse = tossPaymentsService.confirmPayment(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount()
            );

            // 2단계: 검증 결과 확인
            String status = (String) tossResponse.get("status");
            if (!"DONE".equals(status)) {
                throw new RuntimeException("TossPayments 결제 검증 실패: status=" + status);
            }

            // 3단계: DB 저장 (모두 성공해야 함, 하나라도 실패하면 롤백)
            RoomPayment savedPayment = savePayment(request);

            // 4단계: 호텔 예약 정보 저장 (호텔 예약만)
            if ("hotel_reservation".equals(request.getType()) && request.getContentId() != null) {
                saveRoomReservation(request, savedPayment.getOrderIdx());
            }

            // 5단계: Customer 테이블 업데이트 (캐시/포인트 차감)
            updateCustomerBalance(request);

            log.info("결제 및 예약 정보 저장 완료: orderIdx={}", savedPayment.getOrderIdx());

            return PaymentResponseDto.builder()
                    .success(true)
                    .message("결제가 성공적으로 완료되었습니다.")
                    .orderId(request.getOrderId())
                    .paymentKey(request.getPaymentKey())
                    .amount(request.getAmount())
                    .status("DONE")
                    .approvedAt(savedPayment.getApprovedAt())
                    .receiptUrl(savedPayment.getReceiptUrl())
                    .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                    .emailSent(false) // 이메일은 별도 처리
                    .build();

        } catch (RuntimeException e) {
            log.error("결제 처리 실패 - 트랜잭션 롤백: orderId={}", request.getOrderId(), e);
            throw e; // 예외를 다시 던져서 롤백 보장
        } catch (Exception e) {
            log.error("결제 처리 실패 - 트랜잭션 롤백: orderId={}", request.getOrderId(), e);
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private RoomPayment savePayment(PaymentRequestDto request) {
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
        log.info("결제 정보 저장 완료: orderIdx={}", savedPayment.getOrderIdx());
        return savedPayment;
    }

    private void updateCustomerBalance(PaymentRequestDto request) {
        try {
            Customer customer = customerRepository.findById(request.getCustomerIdx())
                    .orElseThrow(() -> new RuntimeException("고객 정보를 찾을 수 없습니다: customerIdx=" + request.getCustomerIdx()));

            // 캐시 차감
            int usedCash = request.getCashUsed() != null ? request.getCashUsed() : 0;
            if (usedCash > 0 && customer.getCash() != null) {
                if (customer.getCash() < usedCash) {
                    throw new RuntimeException("보유 캐시가 부족합니다: 보유=" + customer.getCash() + ", 사용=" + usedCash);
                }
                customer.setCash(customer.getCash() - usedCash);
                log.info("캐시 차감: customerIdx={}, 차감={}, 잔액={}", request.getCustomerIdx(), usedCash, customer.getCash());
            }

            // 포인트 차감
            int usedPoint = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
            if (usedPoint > 0 && customer.getPoint() != null) {
                if (customer.getPoint() < usedPoint) {
                    throw new RuntimeException("보유 포인트가 부족합니다: 보유=" + customer.getPoint() + ", 사용=" + usedPoint);
                }
                customer.setPoint(customer.getPoint() - usedPoint);
                log.info("포인트 차감: customerIdx={}, 차감={}, 잔액={}", request.getCustomerIdx(), usedPoint, customer.getPoint());
            }

            customerRepository.save(customer);
            log.info("고객 잔액 업데이트 완료: customerIdx={}", request.getCustomerIdx());
        } catch (Exception e) {
            log.error("고객 잔액 업데이트 실패", e);
            throw new RuntimeException("고객 잔액 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void saveRoomReservation(PaymentRequestDto request, Integer orderIdx) {
        // TODO: 예약 가능 여부 사전 체크 로직 추가
        // TODO: 객실 재고 확인 및 차감 로직 추가
        // TODO: 체크인/체크아웃 날짜 유효성 검증 강화

        // Room 존재 여부 확인 (복합 키: roomIdx + contentId)
        RoomId roomId = new RoomId(request.getRoomId(), request.getContentId());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException(
                String.format("객실 정보를 찾을 수 없습니다: roomIdx=%d, contentId=%s",
                        request.getRoomId(), request.getContentId())
        ));

        log.info("객실 정보 확인 완료: roomIdx={}, contentId={}, name={}",
                room.getRoomIdx(), room.getContentId(), room.getName());

        RoomReservation reservation = RoomReservation.builder()
                .customerIdx(request.getCustomerIdx())
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
