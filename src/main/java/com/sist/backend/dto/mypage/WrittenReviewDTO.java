package com.sist.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

