package com.sist.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritableReviewDTO {
    private Integer reservationIdx;
    private String hotelName;
    private String location;
    private String contentId;
    private Integer roomIdx;
    private String roomType;
    private String checkOutDate;
    private Long daysLeft;
    
    /**
     * 체크아웃 날짜로부터 남은 일수 계산
     */
    public static WritableReviewDTO fromRoomReservation(ReservationResponseDTO reservation) {
        // 체크아웃 날짜 파싱
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate checkOut = LocalDate.parse(reservation.getCheckOut(), formatter);
        
        // 오늘로부터 체크아웃까지 남은 일수 계산
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), checkOut);
        
        return WritableReviewDTO.builder()
            .reservationIdx(reservation.getId())
            .hotelName(reservation.getHotelName())
            .location(reservation.getLocation())
            .contentId(reservation.getContentId())
            .roomIdx(reservation.getRoomIdx())
            .roomType(reservation.getRoomType())
            .checkOutDate(reservation.getCheckOut())
            .daysLeft(daysLeft)
            .build();
    }
}

