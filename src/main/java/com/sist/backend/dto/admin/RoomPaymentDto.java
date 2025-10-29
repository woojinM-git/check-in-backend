package com.sist.backend.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sist.backend.entity.RoomPayment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomPaymentDto {
    private Integer orderIdx;
    private Integer customerIdx;
    private String roomName; // 예약된 객실명

    private Customer customer;
    private RoomReservation roomReservation;
    private ReservationTime reservationTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private Integer customerIdx;
        private String name;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomReservation {
        private Integer reservIdx;
        private Integer roomIdx;
        private Integer orderIdx;
        private LocalDate checkinDate;
        private LocalDate checkoutDate;
        private Integer guest;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationTime {
        private Integer orderIdx;
        private LocalDateTime inTime;
        private LocalDateTime outTime;
    }

    public static RoomPaymentDto fromEntity(RoomPayment roomPayment) {
        RoomPaymentDto dto = new RoomPaymentDto();
        dto.setOrderIdx(roomPayment.getOrderIdx());
        dto.setCustomerIdx(roomPayment.getCustomerIdx());


        /* Customer 정보 */
        if (roomPayment.getCustomer() != null) {
            Customer customer = new Customer();
            customer.setCustomerIdx(roomPayment.getCustomer().getCustomerIdx());
            customer.setName(roomPayment.getCustomer().getName());
            customer.setPhone(roomPayment.getCustomer().getPhone());
            dto.setCustomer(customer);
        }

        /* RoomReservation 정보 (첫 번째 예약 기준) */
        if (roomPayment.getRoomReservations() != null && !roomPayment.getRoomReservations().isEmpty()) {
            com.sist.backend.entity.RoomReservation first = roomPayment.getRoomReservations().get(0);
            RoomReservation roomReservation = new RoomReservation();
            roomReservation.setReservIdx(first.getReservIdx());
            roomReservation.setRoomIdx(first.getRoomIdx());
            roomReservation.setOrderIdx(first.getOrderIdx());
            roomReservation.setCheckinDate(first.getCheckinDate());
            roomReservation.setCheckoutDate(first.getCheckoutDate());
            roomReservation.setGuest(first.getGuest());
            dto.setRoomReservation(roomReservation);

            // 객실명 추출
            if (first.getRoom() != null) {
                dto.setRoomName(first.getRoom().getName());
            }
        }

        /* ReservationTime 정보 */
        if (roomPayment.getReservationTime() != null) {
            ReservationTime reservationTime = new ReservationTime();
            reservationTime.setOrderIdx(roomPayment.getReservationTime().getOrderIdx());
            reservationTime.setInTime(roomPayment.getReservationTime().getInTime());
            reservationTime.setOutTime(roomPayment.getReservationTime().getOutTime());
            dto.setReservationTime(reservationTime);
        }

        return dto;
    }
}
