package com.sist.backend.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Integer roomIdx;
    private String contentId;
    private String name;
    private Integer capacity;
    private Integer basePrice;
    private Boolean refundable;
    private Boolean breakfastIncluded;
    private Boolean smoking;
    private String imageUrl;
    private Integer status; // 0: 사용불가, 1: 사용가능
}
