package com.sist.backend.dto.mypage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecentViewedHotelRequest {

    @NotBlank
    private String contentId;
}

