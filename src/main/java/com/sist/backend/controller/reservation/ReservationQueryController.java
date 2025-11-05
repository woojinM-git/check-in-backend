package com.sist.backend.controller.reservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.reservation.ReservationCancelDetailDTO;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Query", description = "예약 조회(취소 보강) API")
public class ReservationQueryController {

    private final RoomReservationRepository roomReservationRepository;
    private final RoomPaymentRepository roomPaymentRepository;

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

        Integer pointsUsed = p != null && p.getPointsUsed() != null ? p.getPointsUsed() : 0;
        Integer cashUsed = p != null && p.getCashUsed() != null ? p.getCashUsed() : 0;
        Integer cardPaid = p != null && p.getPrice() != null ? p.getPrice() : 0;
        Integer couponDiscount = 0; // 확실한 쿠폰 금액 정보가 없으므로 0 처리
        Integer totalPrice = r.getTotalPrice() != null ? r.getTotalPrice() : (cardPaid + couponDiscount + pointsUsed + cashUsed);

        String orderNum = null; // 엔티티에 없으면 RES-{reservIdx}
        try {
            // reflection or column not present; keep null
        } catch (Exception ignore) {
        }
        if (orderNum == null) {
            orderNum = "RES-" + reservIdx;
        }

        ReservationCancelDetailDTO dto = ReservationCancelDetailDTO.builder()
                .reservIdx(r.getReservIdx())
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
                .build();
        return ResponseEntity.ok(dto);
    }
}
