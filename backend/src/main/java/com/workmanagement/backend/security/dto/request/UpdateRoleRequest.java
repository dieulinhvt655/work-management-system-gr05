package com.workmanagement.backend.security.dto.request;

import com.workmanagement.backend.common.enums.RoleScope;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateRoleRequest {

    private String name;
    private String description;
    private RoleScope scope;
    private List<Long> permissionIds;

}
