package com.workmanagement.backend.security.dto.response;

import com.workmanagement.backend.common.enums.RoleScope;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private RoleScope scope;
    private List<PermissionResponse> permissions;

}
