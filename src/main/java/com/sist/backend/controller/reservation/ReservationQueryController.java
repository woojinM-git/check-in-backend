package com.sist.backend.controller.reservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.reservation.RefundPolicy;
import com.sist.backend.dto.reservation.ReservationCancelDetailDTO;
import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.CouponRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.entity.DiningPayment;
import com.sist.backend.entity.DiningReservation;
import com.sist.backend.repository.DiningPaymentRepository;
import com.sist.backend.repository.DiningReservationRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Query", description = "예약 조회(취소 보강) API")
@Slf4j
public class ReservationQueryController {

    private final RoomReservationRepository roomReservationRepository;
    private final RoomPaymentRepository roomPaymentRepository;
    private final CouponRepository couponRepository;
    private final DiningReservationRepository diningReservationRepository;
    private final DiningPaymentRepository diningPaymentRepository;

    @GetMapping("/{reservIdx}/detail")
    @Operation(summary = "예약 상세(취소용)", description = "예약 상세를 취소 계산에 필요한 필드와 함께 반환합니다.")
    public ResponseEntity<?> getDetail(@PathVariable Integer reservIdx) {
        RoomReservation r = roomReservationRepository.findById(reservIdx)
                .orElse(null);
        if (r == null) {
            return ResponseEntity.notFound().build();
        }
        RoomPayment p = null;
        if (r.getOrderIdx() != null) {
            p = roomPaymentRepository.findByOrderIdx(r.getOrderIdx()).orElse(null);
        }

        Integer pointsUsed = (p != null && p.getPointsUsed() != null) ? p.getPointsUsed() : 0;
        Integer cashUsed = (p != null && p.getCashUsed() != null) ? p.getCashUsed() : 0;
        Integer cardPaid = (p != null && p.getPrice() != null) ? p.getPrice() : 0;

        // 쿠폰 할인액 조회
        Integer couponDiscount = 0;
        if (p != null && p.getCouponIdx() != null) {
            try {
                Coupon coupon = couponRepository.findById(p.getCouponIdx()).orElse(null);
                if (coupon != null && coupon.getCouponTemplate() != null) {
                    couponDiscount = coupon.getCouponTemplate().getDiscount() != null
                            ? coupon.getCouponTemplate().getDiscount()
                            : 0;
                }
            } catch (Exception ignore) {
                // 쿠폰 할인액 조회 실패 시 0으로 처리
            }
        }

        // totalPrice 재계산: 실제 사용한 모든 금액 합산 (환불 계산용)
        // DB의 totalPrice는 실제 총액과 다를 수 있으므로, 실제 사용 금액을 합산
        Integer totalPrice = cardPaid + cashUsed + pointsUsed;

        // orderNum 필드 매핑
        String orderNum = r.getOrderNum();
        if (orderNum == null || orderNum.isBlank()) {
            orderNum = "RES-" + reservIdx;
        }

        // 환불 예상 정보 계산 (취소 전)
        double expectedRefundRate = calculateRefundRate(r);
        RefundPolicy refundPolicy = RefundPolicy.fromRefundRate(expectedRefundRate);

        // 환불 예상 금액 계산
        int refundable = (int) Math.floor((totalPrice - couponDiscount) * expectedRefundRate);
        int expectedPaymentRefund = Math.min(refundable, cardPaid);
        refundable -= expectedPaymentRefund;
        int expectedCashRestore = refundable > 0 ? Math.min(refundable, cashUsed) : 0;
        refundable -= expectedCashRestore;
        int expectedPointRestore = refundable > 0 ? Math.min(refundable, pointsUsed) : 0;
        int expectedTotalRefund = expectedPaymentRefund + expectedCashRestore + expectedPointRestore;

        // 디버깅 로그
        log.info("[RESERVATION DETAIL] 환불 예상 정보: expectedRefundRate={}, expectedRefundMessage={}, expectedPaymentRefund={}, expectedCashRestore={}, expectedPointRestore={}, expectedTotalRefund={}",
                expectedRefundRate, refundPolicy.getMessage(), expectedPaymentRefund, expectedCashRestore, expectedPointRestore, expectedTotalRefund);

        ReservationCancelDetailDTO dto = ReservationCancelDetailDTO.builder()
                .orderNum(orderNum)
                .hotelTitle(r.getRoom() != null && r.getRoom().getHotelInfo() != null ? r.getRoom().getHotelInfo().getTitle() : null)
                .checkIn(r.getCheckinDate() != null ? r.getCheckinDate().toString() : null)
                .checkOut(r.getCheckoutDate() != null ? r.getCheckoutDate().toString() : null)
                .status(r.getStatus())
                .totalPrice(totalPrice)
                .couponDiscount(couponDiscount)
                .pointsUsed(pointsUsed)
                .cashUsed(cashUsed)
                .cardPaid(cardPaid)
                .expectedRefundRate(expectedRefundRate)
                .expectedRefundMessage(refundPolicy.getMessage())
                .expectedPaymentRefund(expectedPaymentRefund)
                .expectedCashRestore(expectedCashRestore)
                .expectedPointRestore(expectedPointRestore)
                .expectedTotalRefund(expectedTotalRefund)
                .build();
        return ResponseEntity.ok(dto);
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

    @GetMapping("/dining/{diningResrIdx}/detail")
    @Operation(summary = "다이닝 예약 상세(취소용)", description = "다이닝 예약 상세를 취소 계산에 필요한 필드와 함께 반환합니다.")
    public ResponseEntity<?> getDiningDetail(@PathVariable(name = "diningResrIdx") Integer diningResrIdx) {
        DiningReservation r = diningReservationRepository.findById(diningResrIdx)
                .orElse(null);
        if (r == null) {
            return ResponseEntity.notFound().build();
        }
        
        DiningPayment p = null;
        if (r.getDiningpayIdx() != null) {
            p = diningPaymentRepository.findById(r.getDiningpayIdx()).orElse(null);
        }
        
        if (p == null) {
            return ResponseEntity.notFound().build();
        }

        Integer pointsUsed = (p.getPointUsed() != null) ? p.getPointUsed() : 0;
        Integer cashUsed = 0; // 다이닝은 캐시 사용 안 함
        Integer cardPaid = (p.getPrice() != null) ? p.getPrice() : 0;

        // 쿠폰 할인액 조회
        Integer couponDiscount = 0;
        if (p.getCouponIdx() != null) {
            try {
                Coupon coupon = couponRepository.findById(p.getCouponIdx()).orElse(null);
                if (coupon != null && coupon.getCouponTemplate() != null) {
                    couponDiscount = coupon.getCouponTemplate().getDiscount() != null
                            ? coupon.getCouponTemplate().getDiscount()
                            : 0;
                }
            } catch (Exception ignore) {
                // 쿠폰 할인액 조회 실패 시 0으로 처리
            }
        }

        // totalPrice 재계산: 실제 사용한 모든 금액 합산 (환불 계산용)
        // DB의 totalPrice는 실제 총액과 다를 수 있으므로, 실제 사용 금액을 합산
        Integer totalPrice = cardPaid + cashUsed + pointsUsed;

        // 예약번호 생성
        String orderNum = "D-" + diningResrIdx;

        // 환불 예상 정보 계산 (예약 날짜 기준)
        double expectedRefundRate = calculateDiningRefundRate(r);
        RefundPolicy refundPolicy = RefundPolicy.fromRefundRate(expectedRefundRate);

        // 환불 예상 금액 계산
        int refundable = (int) Math.floor((totalPrice - couponDiscount) * expectedRefundRate);
        int expectedPaymentRefund = Math.min(refundable, cardPaid);
        refundable -= expectedPaymentRefund;
        int expectedCashRestore = refundable > 0 ? Math.min(refundable, cashUsed) : 0;
        refundable -= expectedCashRestore;
        int expectedPointRestore = refundable > 0 ? Math.min(refundable, pointsUsed) : 0;
        int expectedTotalRefund = expectedPaymentRefund + expectedCashRestore + expectedPointRestore;

        // 다이닝명 및 호텔명 조회
        String diningName = null;
        String hotelName = null;
        if (r.getDining() != null) {
            diningName = r.getDining().getName();
            if (r.getDining().getHotelInfo() != null) {
                hotelName = r.getDining().getHotelInfo().getTitle();
            }
        }

        ReservationCancelDetailDTO dto = ReservationCancelDetailDTO.builder()
                .orderNum(orderNum)
                .hotelTitle(hotelName != null ? hotelName : diningName) // 호텔명 또는 다이닝명
                .checkIn(r.getReservationDate() != null ? r.getReservationDate().toString() : null)
                .checkOut(null) // 다이닝은 체크아웃 없음
                .status(r.getStatus())
                .totalPrice(totalPrice)
                .couponDiscount(couponDiscount)
                .pointsUsed(pointsUsed)
                .cashUsed(cashUsed)
                .cardPaid(cardPaid)
                .expectedRefundRate(expectedRefundRate)
                .expectedRefundMessage(refundPolicy.getMessage())
                .expectedPaymentRefund(expectedPaymentRefund)
                .expectedCashRestore(expectedCashRestore)
                .expectedPointRestore(expectedPointRestore)
                .expectedTotalRefund(expectedTotalRefund)
                .build();
        return ResponseEntity.ok(dto);
    }

    /**
     * 다이닝 환불 규정 계산 (예약 날짜 기준)
     */
    private double calculateDiningRefundRate(DiningReservation reservation) {
        if (reservation.getReservationDate() == null) {
            return 0.0;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate reservationDate = reservation.getReservationDate();

        // 예약 당일 취소 예외
        if (reservation.getCreatedAt() != null) {
            LocalDate created = reservation.getCreatedAt().toLocalDate();
            if (created.equals(today)) {
                return 1.0;
            }
        }

        long daysBefore = ChronoUnit.DAYS.between(today, reservationDate);
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
