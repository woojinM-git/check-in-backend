package com.sist.backend.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCancelDetailDTO {

    private String orderNum; // fallback to RES-{reservIdx} if null
    private String hotelTitle;
    private String checkIn;   // yyyy-MM-dd
    private String checkOut;  // yyyy-MM-dd
    private Integer status;   // 1 confirmed, 2 cancelled

    private Integer totalPrice;      // reservation.totalPrice (fallback computed)
    private Integer couponDiscount;  // 0 if not applicable
    private Integer pointsUsed;      // RoomPayment.pointsUsed
    private Integer cashUsed;        // RoomPayment.cashUsed
    private Integer cardPaid;        // RoomPayment.price
}
