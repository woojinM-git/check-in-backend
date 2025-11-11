package com.sist.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sist.backend.dto.UsedItemDto;
import com.sist.backend.entity.UsedItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedItemDto {
    // UsedItem 기본 정보
    private Integer usedItemIdx;
    private Integer reservIdx;
    private Integer price;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String comment;
    
    // 예약 정보 (RoomReservation)
    private ReservationInfo reservation;
    
    // 호텔 정보 (HotelInfo)
    private HotelInfo hotel;
    
    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationInfo {
        private LocalDate checkinDate;
        private LocalDate checkoutDate;
        private Integer guest;
        private Integer totalPrice;
        private Integer roomIdx;
        private String contentId;
        private Integer customerIdx;
        private String roomName;
        private String customerNickname;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelInfo {
        private String hotelName;
        private String hotelImageUrl;
        private String hotelAddress;
        private String hotelTel;
    }

    public static UsedItemDto fromEntity(UsedItem usedItem) {
        UsedItemDto dto = new UsedItemDto();
        
        // UsedItem 기본 정보
        dto.setUsedItemIdx(usedItem.getUsedItemIdx());
        dto.setReservIdx(usedItem.getReservIdx());
        dto.setPrice(usedItem.getPrice());
        dto.setStatus(usedItem.getStatus());
        dto.setCreatedAt(usedItem.getCreatedAt());
        dto.setUpdatedAt(usedItem.getUpdatedAt());
        dto.setComment(usedItem.getComment());
        
        // 예약 정보 설정
        if (usedItem.getRoomReservation() != null) {
            // roomReservation이 존재하는 경우 강제로 초기화 (LAZY 로딩 방지)
            usedItem.getRoomReservation().getCheckinDate();
            ReservationInfo reservation = new ReservationInfo();
            reservation.setCheckinDate(usedItem.getRoomReservation().getCheckinDate());
            reservation.setCheckoutDate(usedItem.getRoomReservation().getCheckoutDate());
            reservation.setGuest(usedItem.getRoomReservation().getGuest());
            reservation.setTotalPrice(usedItem.getRoomReservation().getTotalPrice());
            reservation.setRoomIdx(usedItem.getRoomReservation().getRoomIdx());
            reservation.setCustomerIdx(usedItem.getRoomReservation().getCustomerIdx());
            
            // Customer 정보 추가
            if (usedItem.getRoomReservation().getCustomer() != null) {
                reservation.setCustomerNickname(usedItem.getRoomReservation().getCustomer().getNickname());
            }
            
            // Room 정보 추가
            if (usedItem.getRoomReservation().getRoom() != null) {
                reservation.setContentId(usedItem.getRoomReservation().getRoom().getContentId());
                reservation.setRoomName(usedItem.getRoomReservation().getRoom().getName());
                
                // 호텔 정보 설정
                if (usedItem.getRoomReservation().getRoom().getHotelInfo() != null) {
                    HotelInfo hotel = new HotelInfo();
                    hotel.setHotelName(usedItem.getRoomReservation().getRoom().getHotelInfo().getTitle());
                    hotel.setHotelImageUrl(usedItem.getRoomReservation().getRoom().getHotelInfo().getImageUrl());
                    hotel.setHotelAddress(usedItem.getRoomReservation().getRoom().getHotelInfo().getAdress());
                    hotel.setHotelTel(usedItem.getRoomReservation().getRoom().getHotelInfo().getTel());
                    dto.setHotel(hotel);
                }
            }
            
            dto.setReservation(reservation);
        }

        return dto;
    }
}
