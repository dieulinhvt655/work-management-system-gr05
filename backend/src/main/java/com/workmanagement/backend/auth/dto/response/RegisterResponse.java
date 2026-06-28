package com.workmanagement.backend.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponse {

    private Long id;
    private String email;
    private String username;
    private String fullName;

}
