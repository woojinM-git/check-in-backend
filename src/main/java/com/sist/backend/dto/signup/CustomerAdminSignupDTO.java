package com.sist.backend.dto.signup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAdminSignupDTO {
    private Integer customerIdx;
    private Integer adminIdx;
    private String id;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String role;
    private String nickname;
    private LocalDate birthday;
    private String gender;
    private String code;
    private BigDecimal cash;
    private BigDecimal point;
    private BigDecimal totalPrice;
    private String rank;
    private LocalDateTime joinDate;
    private String provider;
    private String status;
    private String refToken;
}
