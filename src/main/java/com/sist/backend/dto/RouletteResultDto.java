package com.sist.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouletteResultDto {
    private Integer prize; // 당첨 금액 (100, 200, 500, 1000, 5000)
    private Integer prizeIndex; // 룰렛 인덱스 (0-4: 100, 200, 500, 1000, 5000)
    private String message; // 결과 메시지
}

