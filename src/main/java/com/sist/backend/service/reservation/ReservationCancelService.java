package com.sist.backend.service.reservation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.reservation.CancelResponseDTO;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelCancelLog;
import com.sist.backend.entity.PointLedger;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.HotelCancelLogRepository;
import com.sist.backend.repository.PointLedgerRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.service.TossPaymentsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCancelService {

    private final RoomReservationRepository roomReservationRepository;
    private final RoomPaymentRepository roomPaymentRepository;
    private final HotelCancelLogRepository hotelCancelLogRepository;
    private final CustomerRepository customerRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final TossPaymentsService tossPaymentsService;

    /**
     * 예약 취소 처리 - 환불 규정 적용 (요구사항 표 준수) - TossPayments 부분 환불 호출 (카드 결제분) -
     * roomReservation/roomPayment 상태 갱신, 취소 로그 기록 - 포인트/캐시 복원 및 PointLedger 기록
     * - Toss 실패 시 전체 롤백
     */
    @Transactional(rollbackFor = Exception.class)
    public CancelResponseDTO cancelReservation(Integer reservIdx, String cancelReason) {
        log.info("[CANCEL] start: reservIdx={}, reason={}", reservIdx, cancelReason);

        // 1) 예약/결제 조회
        RoomReservation reservation = roomReservationRepository.findById(reservIdx)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        RoomPayment payment = roomPaymentRepository.findByOrderIdx(reservation.getOrderIdx())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 2) 환불 비율 계산
        double refundRate = calculateRefundRate(reservation);
        log.info("[CANCEL] refundRate={}", refundRate);

        int totalPrice = reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0;
        int refundTotalAmount = (int) Math.floor(totalPrice * refundRate);

        int pointsUsed = payment.getPointsUsed() != null ? payment.getPointsUsed() : 0;
        int cashUsed = payment.getCashUsed() != null ? payment.getCashUsed() : 0;

        int refundPoint = (int) Math.floor(pointsUsed * refundRate);
        int refundCash = (int) Math.floor(cashUsed * refundRate);

        // 결제 취소 대상 금액(카드 금액)을 기준으로 비율 적용
        // payment.price는 실제 카드 결제 금액으로 저장되어 있음
        int cardPaid = payment.getPrice() != null ? payment.getPrice() : 0;
        int refundCardAmount = Math.max(0, refundTotalAmount - refundPoint - refundCash);

        // 3) 상태 선반영 (취소 처리)
        reservation.setStatus(2); // 취소
        roomReservationRepository.save(reservation);

        payment.setStatus(2); // 취소
        roomPaymentRepository.save(payment);

        // 4) 취소 로그 생성 (Toss 전 상태: 0)
        HotelCancelLog preLog = HotelCancelLog.builder()
                .reservIdx(reservIdx)
                .cancelReason(cancelReason)
                .refundTotalAmount(refundTotalAmount)
                .refundCash(refundCash)
                .refundPoint(refundPoint)
                .refundStatus(0)
                .canceledBy("USER")
                .cancelAt(java.time.LocalDateTime.now())
                .build();
        preLog = hotelCancelLogRepository.save(preLog);

        // 5) Toss 환불 호출 (부분 취소 금액)
        try {
            if (refundCardAmount > 0) {
                Map<String, Object> tossRes = tossPaymentsService.cancelPaymentWithAmount(payment.getPaymentKey(), refundCardAmount, cancelReason);
                log.info("[CANCEL] Toss refund ok: status={}", tossRes.get("status"));
            } else {
                log.info("[CANCEL] 카드 환불 금액 없음, 포인트/캐시 복원만 수행");
            }

            // 6) 포인트/캐시 되돌려주기 + 이력
            if (refundPoint > 0 || refundCash > 0) {
                Customer customer = customerRepository.findById(payment.getCustomerIdx())
                        .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));

                if (refundCash > 0) {
                    int currentCash = customer.getCash() != null ? customer.getCash() : 0;
                    customer.setCash(currentCash + refundCash);
                }
                if (refundPoint > 0) {
                    int currentPoint = customer.getPoint() != null ? customer.getPoint() : 0;
                    customer.setPoint(currentPoint + refundPoint);

                    PointLedger ledger = PointLedger.builder()
                            .customerIdx(customer.getCustomerIdx())
                            .orderIdx(payment.getOrderIdx())
                            .point(refundPoint)
                            .pointType("복원")
                            .memo("예약 취소 환불")
                            .build();
                    pointLedgerRepository.save(ledger);
                }
                customerRepository.save(customer);
            }

            // 7) 로그 완료 처리
            preLog.setRefundStatus(1);
            hotelCancelLogRepository.save(preLog);

            return CancelResponseDTO.builder()
                    .message("success")
                    .reservIdx(reservIdx)
                    .refundRate(refundRate)
                    .refundTotalAmount(refundTotalAmount)
                    .refundCash(refundCash)
                    .refundPoint(refundPoint)
                    .refundStatus(1)
                    .cancelReason(cancelReason)
                    .build();
        } catch (Exception e) {
            log.error("[CANCEL] Toss refund failed: {}", e.getMessage(), e);
            preLog.setRefundStatus(2);
            hotelCancelLogRepository.save(preLog);
            throw e; // 트랜잭션 롤백
        }
    }

    /**
     * 환불 규정 계산
     */
    private double calculateRefundRate(RoomReservation reservation) {
        LocalDate today = LocalDate.now();
        LocalDate checkin = reservation.getCheckinDate();

        // 예약 당일 취소 예외: createdAt이 있고 같은 날짜면 100%
        if (reservation.getCreatedAt() != null) {
            LocalDate created = reservation.getCreatedAt().toLocalDate();
            if (created.equals(today)) {
                return 1.0;
            }
        }

        long daysBefore = ChronoUnit.DAYS.between(today, checkin);
        if (daysBefore >= 7) {
            return 1.0;
        }
        if (daysBefore >= 3) {
            return 0.5;
        }
        if (daysBefore >= 1) {
            return 0.3;
        }
        return 0.0;
    }
}
