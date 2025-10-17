package com.sist.backend.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {

    private String contentId;
    private String title;
    private String adress;
    private String imageUrl;
    private String areaCode;

    // Nested detail (optional fields)
    private String roomcount;
    private String foodplace;
    private String parkinglodging;
    private String reservationlodging;
    private String scalelodging;
}
