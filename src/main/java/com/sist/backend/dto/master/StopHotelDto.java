package com.sist.backend.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StopHotelDto {
    /* 정지할 떄 업데이트 정보들 */
    private String contentId;
    private Integer status;
    private String reason;
}
