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
public class RoomResponse {

    private String contentId;
    private String name;
    private Integer capacity;
    private BigDecimal basePrice;
    private Boolean refundable;
    private Boolean breakfastIncluded;
    private Boolean smoking;
    private String imageUrl;
}
