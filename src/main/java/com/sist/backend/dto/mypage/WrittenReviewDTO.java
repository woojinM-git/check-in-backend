package com.sist.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrittenReviewDTO {
    private Integer reviewIdx;
    private Integer reservationIdx;
    private String hotelName;
    private String location;
    private String contentId;
    private Integer roomIdx;
    private String roomType;
    private BigDecimal star;
    private String content;
    private Integer helpfulCount;
    private String date;
    private LocalDateTime createdAt;
    
    // 예약 원시 날짜 값 (프론트에서 포맷/박수 계산용)
    private LocalDate checkInDate;   // roomReservation.checkinDate
    private LocalDate checkOutDate;  // roomReservation.checkoutDate
    
    // 표시용 부가 정보
    private String region;           // 시/도 (Area.areaName 또는 adress 첫 토큰)
    private String thumbnailUrl;     // 호텔 썸네일 URL
    
    // 호텔 정보 객체를 위한 필드 추가
    private HotelInfoDTO hotelInfo;
    
    @Data
    public static class HotelInfoDTO {
        private String contentId;
        private String title;
        private String adress;
        private String tel;
    }
}

