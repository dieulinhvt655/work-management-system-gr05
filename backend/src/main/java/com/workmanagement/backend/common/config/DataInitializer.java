package com.workmanagement.backend.common.config;

import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.entity.RolePermission;
import com.workmanagement.backend.security.repository.PermissionRepository;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seed permissions, role-permission mappings và tài khoản admin mặc định.
 * Idempotent — an toàn khi restart app.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@workmanagement.local";
    private static final String ADMIN_PASSWORD = "admin123";

    /** Roles luôn được đồng bộ lại permission khi restart (thêm/sửa quyền trong seed). */
    private static final Set<String> ROLES_ALWAYS_RESYNC = Set.of("System Admin", "Workspace Owner");

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedRolePermissions();
        seedAdminUser();
        log.info("DataInitializer completed");
    }

    private void seedPermissions() {
        for (String[] row : PERMISSIONS) {
            String code = row[0];
            if (permissionRepository.existsByCode(code)) {
                continue;
            }
            permissionRepository.save(Permission.builder()
                    .code(code)
                    .name(row[1])
                    .module(row[2])
                    .description(row[3])
                    .build());
        }
    }

    private void seedRoles() {
        for (String[] row : ROLES) {
            RoleScope scope = RoleScope.fromValue(row[2]);
            if (roleRepository.findByNameAndScope(row[0], scope).isPresent()) {
                continue;
            }
            roleRepository.save(Role.builder()
                    .name(row[0])
                    .description(row[1])
                    .scope(scope)
                    .build());
        }
    }

    private void seedRolePermissions() {
        Map<String, Permission> byCode = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, p -> p, (a, b) -> a));

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS.entrySet()) {
            Role role = findRole(entry.getKey());
            boolean alwaysResync = ROLES_ALWAYS_RESYNC.contains(role.getName());
            if (!alwaysResync && !rolePermissionRepository.findByRole_Id(role.getId()).isEmpty()) {
                continue;
            }
            rolePermissionRepository.deleteByRole_Id(role.getId());
            for (String code : entry.getValue()) {
                Permission permission = byCode.get(code);
                if (permission != null) {
                    rolePermissionRepository.save(new RolePermission(role, permission));
                }
            }
        }
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        Role systemAdmin = findRole("System Admin");
        userRepository.save(User.builder()
                .fullName("System Admin")
                .email(ADMIN_EMAIL)
                .username("admin")
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .status(UserStatus.ACTIVE)
                .role(systemAdmin)
                .build());
        log.info("Seeded admin user: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private Role findRole(String name) {
        return roleRepository.findAll().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Role not found: " + name));
    }

    private static final String[][] PERMISSIONS = {
            {"user:read", "Xem người dùng", "user", "Xem danh sách và chi tiết tài khoản"},
            {"user:create", "Tạo người dùng", "user", "Tạo tài khoản người dùng mới"},
            {"user:update", "Cập nhật người dùng", "user", "Cập nhật thông tin tài khoản"},
            {"user:lock", "Khoá/Mở khoá người dùng", "user", "Thay đổi trạng thái tài khoản"},
            {"user:assign-role", "Gán vai trò người dùng", "user", "Gán vai trò hệ thống cho user"},
            {"role:read", "Xem vai trò", "role", "Xem danh sách và chi tiết vai trò"},
            {"role:create", "Tạo vai trò", "role", "Tạo vai trò mới"},
            {"role:update", "Cập nhật vai trò", "role", "Cập nhật thông tin vai trò"},
            {"role:delete", "Xóa vai trò", "role", "Xóa vai trò"},
            {"role:assign-permission", "Gán quyền cho vai trò", "role", "Gán permission cho role"},
            {"permission:read", "Xem quyền", "permission", "Xem danh sách quyền"},
            {"permission:create", "Tạo quyền", "permission", "Tạo quyền mới"},
            {"permission:update", "Cập nhật quyền", "permission", "Cập nhật quyền"},
            {"permission:delete", "Xóa quyền", "permission", "Xóa quyền"},
            {"workspace:read", "Xem workspace", "workspace", "Xem thông tin workspace"},
            {"workspace:create", "Tạo workspace", "workspace", "Tạo workspace mới"},
            {"workspace:update", "Cập nhật workspace", "workspace", "Cập nhật thông tin workspace"},
            {"workspace:delete", "Xóa workspace", "workspace", "Xóa workspace"},
            {"workspace:close", "Đóng workspace", "workspace", "Vô hiệu hoá workspace"},
            {"team:read", "Xem nhóm", "team", "Xem thông tin nhóm làm việc"},
            {"team:create", "Tạo nhóm", "team", "Tạo nhóm làm việc"},
            {"team:update", "Cập nhật nhóm", "team", "Cập nhật nhóm làm việc"},
            {"team:delete", "Giải thể nhóm", "team", "Giải thể nhóm làm việc"},
            {"project:read", "Xem dự án", "project", "Xem thông tin dự án"},
            {"project:create", "Tạo dự án", "project", "Tạo dự án mới"},
            {"project:update", "Cập nhật dự án", "project", "Cập nhật thông tin dự án"},
            {"project:delete", "Xóa dự án", "project", "Xóa dự án"},
            {"project:manage-members", "Quản lý thành viên dự án", "project", "Thêm/gỡ thành viên dự án"},
            {"backlog:read", "Xem backlog", "backlog", "Tra cứu Product Backlog Item"},
            {"backlog:create", "Tạo backlog item", "backlog", "Tạo PBI mới"},
            {"backlog:update", "Cập nhật backlog item", "backlog", "Cập nhật PBI"},
            {"backlog:delete", "Xóa backlog item", "backlog", "Xóa PBI"},
            {"sprint:read", "Xem sprint", "sprint", "Xem thông tin sprint"},
            {"sprint:create", "Tạo sprint", "sprint", "Tạo sprint mới"},
            {"sprint:update", "Cập nhật sprint", "sprint", "Cập nhật sprint"},
            {"sprint:delete", "Xóa sprint", "sprint", "Xóa sprint"},
            {"task:read", "Xem task", "task", "Xem công việc"},
            {"task:create", "Tạo task", "task", "Tạo công việc"},
            {"task:update", "Cập nhật task", "task", "Cập nhật công việc"},
            {"task:delete", "Xóa task", "task", "Xóa công việc"},
            {"task:assign", "Gán task", "task", "Phân công công việc"},
            {"comment:read", "Xem bình luận", "comment", "Xem trao đổi"},
            {"comment:create", "Tạo bình luận", "comment", "Tạo bình luận trao đổi"},
            {"comment:update", "Sửa bình luận", "comment", "Chỉnh sửa bình luận"},
            {"comment:delete", "Xóa bình luận", "comment", "Xóa bình luận"},
            {"attachment:read", "Xem tệp đính kèm", "attachment", "Xem danh sách tệp"},
            {"attachment:create", "Tải lên tệp", "attachment", "Upload tệp đính kèm"},
            {"attachment:delete", "Xóa tệp", "attachment", "Xóa tệp đính kèm"},
            {"notification:read", "Xem thông báo", "notification", "Xem thông báo hệ thống"},
            {"dashboard:read", "Xem dashboard", "dashboard", "Xem dashboard tổng quan"},
    };

    private static final String[][] ROLES = {
            {"System Admin", "Quản trị viên hệ thống", "system"},
            {"Workspace Owner", "Người sở hữu workspace", "workspace"},
            {"Workspace Member", "Thành viên workspace", "workspace"},
            {"Team Leader", "Trưởng nhóm", "team"},
            {"Team Member", "Thành viên nhóm", "team"},
            {"Project Manager", "Quản lý dự án", "project"},
            {"Project Contributor", "Thành viên dự án", "project"},
    };

    private static final Map<String, List<String>> ROLE_PERMISSIONS = buildRolePermissions();

    private static Map<String, List<String>> buildRolePermissions() {
        List<String> all = Arrays.stream(PERMISSIONS).map(row -> row[0]).toList();

        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("System Admin", all);

        map.put("Workspace Owner", List.of(
                "user:read", "user:create", "user:update", "user:lock", "user:assign-role",
                "role:read",
                "workspace:read", "workspace:create", "workspace:update", "workspace:close",
                "team:read", "team:create", "team:update", "team:delete",
                "project:read", "attachment:read", "dashboard:read", "notification:read"
        ));

        map.put("Workspace Member", List.of(
                "workspace:read", "team:read", "project:read", "attachment:read",
                "task:read", "comment:read", "comment:create", "comment:update",
                "dashboard:read", "notification:read"
        ));

        map.put("Team Leader", List.of(
                "team:read", "team:create", "team:update", "team:delete",
                "project:read", "project:create", "project:update", "project:manage-members",
                "attachment:read", "attachment:create", "attachment:delete",
                "backlog:read", "sprint:read",
                "task:read", "task:create", "task:update", "task:assign",
                "comment:read", "comment:create", "comment:update",
                "dashboard:read", "notification:read"
        ));

        map.put("Team Member", List.of(
                "team:read", "project:read", "attachment:read",
                "task:read", "task:update",
                "comment:read", "comment:create", "comment:update",
                "dashboard:read", "notification:read"
        ));

        map.put("Project Manager", List.of(
                "project:read", "project:create", "project:update", "project:manage-members",
                "backlog:read", "backlog:create", "backlog:update", "backlog:delete",
                "sprint:read", "sprint:create", "sprint:update", "sprint:delete",
                "task:read", "task:create", "task:update", "task:delete", "task:assign",
                "comment:read", "comment:create", "comment:update", "comment:delete",
                "attachment:read", "attachment:create", "attachment:delete",
                "dashboard:read", "notification:read"
        ));

        map.put("Project Contributor", List.of(
                "project:read",
                "task:read", "task:update",
                "comment:read", "comment:create", "comment:update",
                "attachment:read",
                "dashboard:read", "notification:read"
        ));

        return map;
    }

}
