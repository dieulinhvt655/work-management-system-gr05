package com.workmanagement.backend.security.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePermissionRequest {

    private String name;
    private String module;
    private String description;

}
