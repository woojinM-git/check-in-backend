package com.sist.backend.dto.hotel;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HotelShareListResponse {
    private final String contentId;
    private final String title;
    private final String address;
    private final String imageUrl;
    private final String areaCode;
    private final BigDecimal mapX;
    private final BigDecimal mapY;
    private final Integer minPrice;
}
