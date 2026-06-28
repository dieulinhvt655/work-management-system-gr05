package com.workmanagement.backend.security.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {

    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;

}
