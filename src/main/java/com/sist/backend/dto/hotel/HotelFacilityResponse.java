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
public class HotelFacilityResponse {

    private String contentId;
    private String parkinglodging;
    private String foodplace;
    private String reservationlodging;
    private String scalelodging;
    private BigDecimal mapX;
    private BigDecimal mapY;
}
