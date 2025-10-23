package com.sist.backend.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sist.backend.entity.RoomReservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomReservationDto {
    /* RoomReservation 기본 정보 */
    private Integer reservIdx;
    private Integer roomIdx;
    private String contentid;
    private Integer customerIdx;
    private Integer orderIdx;
    private Integer status;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer guest;
    private Integer totalPrice;
    private String qrUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* Room 정보 */
    private Room room;

    /* Customer 정보 */
    private Customer customer;

    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Room {
        private Integer roomIdx;
        private String name;
        private Integer basePrice;
        private Integer status; // 0: 사용불가, 1: 사용가능
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private Integer customerIdx;
        private String name;
        private String phone;
    }

    public static RoomReservationDto fromEntity(RoomReservation roomReservation) {
        RoomReservationDto dto = new RoomReservationDto();
        dto.setReservIdx(roomReservation.getReservIdx());
        dto.setRoomIdx(roomReservation.getRoomIdx());
        dto.setContentid(roomReservation.getContentid());
        dto.setCustomerIdx(roomReservation.getCustomerIdx());
        dto.setOrderIdx(roomReservation.getOrderIdx());
        dto.setStatus(roomReservation.getStatus());
        dto.setCheckinDate(roomReservation.getCheckinDate());
        dto.setCheckoutDate(roomReservation.getCheckoutDate());
        dto.setGuest(roomReservation.getGuest());
        dto.setTotalPrice(roomReservation.getTotalPrice());
        dto.setQrUrl(roomReservation.getQrUrl());
        dto.setCreatedAt(roomReservation.getCreatedAt());
        dto.setUpdatedAt(roomReservation.getUpdatedAt());

        /* Room 정보 */
        if (roomReservation.getRoom() != null) {
            Room room = new Room();
            room.setRoomIdx(roomReservation.getRoom().getRoomIdx());
            room.setName(roomReservation.getRoom().getName());
            room.setBasePrice(roomReservation.getRoom().getBasePrice());
            room.setStatus(roomReservation.getRoom().getStatus());
            dto.setRoom(room);
        }
        /* Customer 정보 */
        if (roomReservation.getCustomer() != null) {
            Customer customer = new Customer();
            customer.setCustomerIdx(roomReservation.getCustomer().getCustomerIdx());
            customer.setName(roomReservation.getCustomer().getName());
            customer.setPhone(roomReservation.getCustomer().getPhone());
            dto.setCustomer(customer);
        }
        return dto;
    }
}
