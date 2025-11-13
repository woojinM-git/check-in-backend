package com.sist.backend.dto.login;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
    private String userId;
    private String newPassword;
}

