package com.sist.backend.dto.mypage;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RecentViewedHotelResponse {

    Integer recentViewedIdx;
    Integer customerIdx;
    String contentId;
    String hotelName;
    String address;
    String imageUrl;
    Integer minPrice;
    LocalDateTime viewedAt;
}

