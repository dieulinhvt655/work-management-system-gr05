package com.workmanagement.backend.auth.dto.response;

import com.workmanagement.backend.security.dto.response.RoleResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String username;
    private RoleResponse role;

}
