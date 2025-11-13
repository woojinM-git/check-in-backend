package com.sist.backend.dto.login;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetVerifyCodeRequest {
    private String name;
    private String userId;
    private String email;
    private String code;
}

