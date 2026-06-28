package com.workmanagement.backend.security.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    private Permission permission;

    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
        this.id = new RolePermissionId(role.getId(), permission.getId());
    }

}
