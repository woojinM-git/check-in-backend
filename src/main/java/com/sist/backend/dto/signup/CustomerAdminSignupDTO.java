package com.sist.backend.dto.signup;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAdminSignupDTO {
    private String id;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String role;
    private String nickname;
    private LocalDate birthday;
    private String gender;

}
