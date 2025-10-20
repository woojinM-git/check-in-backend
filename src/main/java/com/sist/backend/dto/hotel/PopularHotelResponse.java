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
public class PopularHotelResponse {
    private String contentId;
    private String title;
    private String adress;
    private String imageUrl;
    private String areaCode;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
