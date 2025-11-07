package com.sist.backend.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomImageResponse {

    private Integer roomImageIdx;
    private Integer roomIdx;
    private String contentId;
    private String imageUrl;
    private Integer imageOrder;
}
