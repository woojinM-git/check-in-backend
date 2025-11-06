package com.sist.backend.service.reservation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.reservation.CancelResponseDTO;
import com.sist.backend.dto.reservation.RefundPolicy;
import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelCancelLog;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.CouponRepository;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.HotelCancelLogRepository;
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
    private final TossPaymentsService tossPaymentsService;
    private final CouponRepository couponRepository;

    /**
     * 예약 취소 처리 - 환불 규정 적용 (요구사항 표 준수) - TossPayments 부분 환불 호출 (카드 결제분) -
     * roomReservation/roomPayment 상태 갱신, 취소 로그 기록 - 포인트/캐시 복원 (hotelCancelLog에만
     * 기록) - customer 테이블의 point, cash, totalPrice 업데이트 - Toss 실패 시 전체 롤백
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
        RefundPolicy refundPolicy = RefundPolicy.fromRefundRate(refundRate);
        log.info("[CANCEL] refundRate={}, policy={}", refundRate, refundPolicy);

        // 3) 쿠폰 할인액 조회
        int couponDiscount = 0;
        if (payment.getCouponIdx() != null) {
            couponDiscount = getCouponDiscount(payment.getCouponIdx());
        }

        // 4) 환불 금액 계산 (사용자 요구사항 로직)
        int totalPrice = reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0;
        int pointsUsed = (payment.getPointsUsed() != null) ? payment.getPointsUsed() : 0;
        int cashUsed = (payment.getCashUsed() != null) ? payment.getCashUsed() : 0;
        int cardPaid = (payment.getPrice() != null) ? payment.getPrice() : 0;

        RefundCalculationResult refundResult = calculateRefund(
                totalPrice, couponDiscount, cardPaid, cashUsed, pointsUsed, refundRate);

        int refundTotalAmount = refundResult.getTotalRefund();
        int refundCardAmount = refundResult.getPaymentRefund();
        int refundCash = refundResult.getCashRestore();
        int refundPoint = refundResult.getPointRestore();

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

            // 6) 포인트/캐시 복원 및 customer 테이블 업데이트
            // 환불 금액이 있으면 customer 테이블 업데이트
            if (refundTotalAmount > 0) {
                Customer customer = customerRepository.findById(payment.getCustomerIdx())
                        .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다."));

                // 포인트 복원
                if (refundPoint > 0) {
                    int currentPoint = (customer.getPoint() != null) ? customer.getPoint() : 0;
                    customer.setPoint(currentPoint + refundPoint);
                }

                // 캐시 복원
                if (refundCash > 0) {
                    int currentCash = (customer.getCash() != null) ? customer.getCash() : 0;
                    customer.setCash(currentCash + refundCash);
                }

                // 실제 결제 금액 차감 (totalPrice에서 환불 금액 차감)
                if (refundCardAmount > 0) {
                    int currentTotalPrice = (customer.getTotalPrice() != null) ? customer.getTotalPrice() : 0;
                    customer.setTotalPrice(Math.max(0, currentTotalPrice - refundCardAmount));
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
                    .refundMessage(refundPolicy.getMessage())
                    .refundTotalAmount(refundTotalAmount)
                    .paymentRefund(refundCardAmount)
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

    /**
     * 쿠폰 할인액 조회
     */
    private int getCouponDiscount(Integer couponIdx) {
        if (couponIdx == null) {
            return 0;
        }
        try {
            Coupon coupon = couponRepository.findById(couponIdx).orElse(null);
            if (coupon != null && coupon.getCouponTemplate() != null) {
                Integer discount = coupon.getCouponTemplate().getDiscount();
                return (discount != null) ? discount : 0;
            }
        } catch (Exception e) {
            log.warn("[CANCEL] 쿠폰 할인액 조회 실패: couponIdx={}, error={}", couponIdx, e.getMessage());
        }
        return 0;
    }

    /**
     * 환불 계산 (사용자 요구사항 로직) 기준 환불액 = (totalPrice - coupon) * refundRate 실 결제 환불 =
     * min(기준 환불액, amountPaid) 캐시 복원 = min(남은 금액, cashUsed) 포인트 복원 = min(남은 금액,
     * pointUsed)
     */
    private RefundCalculationResult calculateRefund(
            int totalPrice, int couponDiscount, int amountPaid,
            int cashUsed, int pointUsed, double refundRate) {

        // 기준 환불액 = (총액 - 쿠폰 할인) * 환불율
        int refundable = (int) Math.floor((totalPrice - couponDiscount) * refundRate);

        int paymentRefund = 0;
        int cashRestore = 0;
        int pointRestore = 0;

        // 1. 실 결제 환불
        paymentRefund = Math.min(refundable, amountPaid);
        refundable -= paymentRefund;

        // 2. 캐시 복원
        if (refundable > 0) {
            cashRestore = Math.min(refundable, cashUsed);
            refundable -= cashRestore;
        }

        // 3. 포인트 복원
        if (refundable > 0) {
            pointRestore = Math.min(refundable, pointUsed);
        }

        int totalRefund = paymentRefund + cashRestore + pointRestore;

        return new RefundCalculationResult(totalRefund, paymentRefund, cashRestore, pointRestore);
    }

    /**
     * 환불 계산 결과 클래스
     */
    private static class RefundCalculationResult {

        private final int totalRefund;
        private final int paymentRefund;
        private final int cashRestore;
        private final int pointRestore;

        public RefundCalculationResult(int totalRefund, int paymentRefund, int cashRestore, int pointRestore) {
            this.totalRefund = totalRefund;
            this.paymentRefund = paymentRefund;
            this.cashRestore = cashRestore;
            this.pointRestore = pointRestore;
        }

        public int getTotalRefund() {
            return totalRefund;
        }

        public int getPaymentRefund() {
            return paymentRefund;
        }

        public int getCashRestore() {
            return cashRestore;
        }

        public int getPointRestore() {
            return pointRestore;
        }
    }
}
