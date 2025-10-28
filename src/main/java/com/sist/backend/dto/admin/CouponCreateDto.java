package com.sist.backend.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateDto {
    private Integer templateIdx;
    private Integer customerIdx;
}
