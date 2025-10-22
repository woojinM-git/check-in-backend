package com.sist.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프로필 업데이트 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String nickname;
    private String phone;
    private String email;
}
