package com.sist.backend.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private Integer customerIdx;
    private String id;
    private String name; // 실명 추가
    private String nickname;
    private String email;
    private String phone;
    private BigDecimal cash;
    private BigDecimal point;
    private BigDecimal totalPrice;
    private String rank;
    private LocalDateTime joinDate;
    private Integer provider;
}
