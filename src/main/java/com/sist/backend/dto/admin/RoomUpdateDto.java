package com.sist.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateDto {
    private Integer roomIdx;
    private String name;
    private Integer capacity;
    private Integer basePrice;
}
