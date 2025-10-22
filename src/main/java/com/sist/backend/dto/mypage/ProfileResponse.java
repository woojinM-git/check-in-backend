package com.sist.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로필 조회 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Integer customerIdx;
    private String id;
    private String nickname;
    private String name;
    private String phone;
    private String email;
    private String gender;
    private LocalDate birthday;
    private String rank;
    private Integer cash;
    private Integer point;
    private Integer totalPrice;
    private LocalDateTime joinDate;
}
