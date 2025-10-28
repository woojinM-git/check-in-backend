package com.sist.backend.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectHotelRequestDto {
    // RegistrationRequest 기본 정보
    private Integer registrationIdx;
    private String refusalMsg;
}
