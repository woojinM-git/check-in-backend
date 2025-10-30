package com.sist.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckTimeUpdateDto {
    private Integer orderIdx;
    private LocalDateTime inTime;
    private LocalDateTime outTime;
}
