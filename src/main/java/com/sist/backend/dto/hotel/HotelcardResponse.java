package com.sist.backend.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelcardResponse {
    private String contentId;
    private String title;
    private String adress;
    private String imageUrl;
    private String areaCode;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double distance; // 다트게임에서 사용할 거리 정보 (킬로미터)
}
