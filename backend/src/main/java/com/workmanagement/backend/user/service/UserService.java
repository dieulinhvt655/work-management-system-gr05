package com.workmanagement.backend.user.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.auth.service.RefreshTokenService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.team.service.TeamMemberService;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.repository.WorkspaceRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
import com.workmanagement.backend.user.dto.request.CreateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateProfileRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserStatusRequest;
import com.workmanagement.backend.user.dto.response.UserResponse;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.mapper.UserMapper;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.user.util.EmployeeCodeGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9 .()-]{5,19}$");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile(".*[^A-Za-z0-9].*");
    private static final String DEFAULT_USER_ROLE_NAME = "Workspace Member";
    private static final String DEFAULT_TEAM_MEMBER_ROLE_NAME = "Team Member";
    private static final String SYSTEM_ADMIN_ROLE_NAME = "System Admin";
    private static final String WORKSPACE_OWNER_ROLE_NAME = "Workspace Owner";
    private static final Set<String> USER_SORT_FIELDS = Set.of(
            "id", "fullName", "email", "username", "employeeCode", "status", "createdAt", "updatedAt"
    );

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final RefreshTokenService refreshTokenService;
    private final WorkspaceService workspaceService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TeamRepository teamRepository;
    private final EmployeeCodeGenerator employeeCodeGenerator;
    private final TeamMemberService teamMemberService;

    /** UC-1.4 — Xem hồ sơ cá nhân */
    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        return toDetailResponse(getCurrentUser());
    }

    /** UC-1.4 — Cập nhật hồ sơ cá nhân */
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        validateProfile(request);

        List<String> changedFields = new ArrayList<>();
        if (request.getPhone() != null) {
            String newPhone = normalizeOptionalValue(request.getPhone());
            if (!Objects.equals(user.getPhone(), newPhone)) {
                user.setPhone(newPhone);
                changedFields.add("phone");
            }
        }
        if (request.getAvatarUrl() != null) {
            String newAvatarUrl = normalizeOptionalValue(request.getAvatarUrl());
            if (!Objects.equals(user.getAvatarUrl(), newAvatarUrl)) {
                user.setAvatarUrl(newAvatarUrl);
                changedFields.add("avatarUrl");
            }
        }
        if (request.getDescription() != null) {
            String newDescription = normalizeOptionalValue(request.getDescription());
            if (!Objects.equals(user.getDescription(), newDescription)) {
                user.setDescription(newDescription);
                changedFields.add("description");
            }
        }

        if (StringUtils.hasText(request.getNewPassword())) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng nhập mật khẩu hiện tại");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không đúng");
            }

            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Xác nhận mật khẩu mới không khớp");
            }

            validatePasswordPolicy(request.getNewPassword());

            if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Mật khẩu mới không được trùng với mật khẩu hiện tại"
                );
            }

            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            changedFields.add("password");
        }

        if (changedFields.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không có thông tin nào thay đổi");
        }

        User savedUser = userRepository.save(user);
        activityLogService.recordOrgEvent(
                user.getId(),
                ActivityLogAction.USER_PROFILE_UPDATED,
                ActivityLogAction.TARGET_USER,
                user.getId(),
                String.join(",", changedFields)
        );

        return toDetailResponse(savedUser);
    }

    /** UC-1.5 — Tạo tài khoản người dùng */
    @Transactional
    @PreAuthorize("hasAuthority('user:create')")
    public UserResponse create(CreateUserRequest request) {
        validateCreateRequest(request);

        String employeeCode = employeeCodeGenerator.generateUnique();
        String email = request.getEmail().trim().toLowerCase();
        String username = resolveUsername(request, employeeCode);
        String rawPassword = generateDefaultPassword(username);
        validatePasswordPolicy(rawPassword);

        validateUniqueEmail(email, null);
        validateUniqueUsername(username, null);

        Workspace workspace = workspaceService.getWorkspace(request.getWorkspaceId());
        workspaceService.verifyCanManage(workspace);
        ensureWorkspaceActive(workspace);
        if (request.getTeamId() != null) {
            ensureTeamInWorkspace(request.getTeamId(), workspace.getId());
        }

        Role userRole = resolveUserRole(request);
        Role workspaceMemberRole = resolveWorkspaceMemberRole(userRole);

        User currentUser = getCurrentUser();
        if (userRole.getScope() == RoleScope.SYSTEM && !isSystemAdmin(currentUser)) {
            throw new AccessDeniedException("Chỉ System Admin mới được tạo tài khoản có vai trò hệ thống");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .username(username)
                .employeeCode(employeeCode)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .phone(request.getPhone() != null ? normalizeOptionalValue(request.getPhone()) : null)
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .role(userRole)
                .build();
        user = userRepository.save(user);

        WorkspaceMember workspaceMember = workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(workspaceMemberRole)
                .addedByOwner(currentUser)
                .status(MemberStatus.ACTIVE)
                .build());

        Long actorId = currentUser.getId();
        activityLogService.recordOrgEvent(
                actorId,
                ActivityLogAction.USER_CREATED,
                ActivityLogAction.TARGET_USER,
                user.getId(),
                user.getEmail()
        );
        activityLogService.recordOrgEvent(
                actorId,
                ActivityLogAction.WORKSPACE_MEMBER_ADDED,
                ActivityLogAction.TARGET_WORKSPACE_MEMBER,
                workspaceMember.getId(),
                user.getFullName()
        );

        if (request.getTeamId() != null) {
            assignToTeam(workspace.getId(), request.getTeamId(), workspaceMember);
        }

        return toDetailResponse(user);
    }

    /** UC-1.6 — Xem danh sách tài khoản */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:read')")
    public PageResponse<UserResponse> findAll(
            int page,
            int size,
            String keyword,
            UserStatus status,
            Long workspaceId,
            Long roleId,
            Long teamId,
            String sortBy,
            String sortDirection
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = validateSortField(sortBy);
        Sort.Direction direction = parseSortDirection(sortDirection);
        UserAccessScope accessScope = resolveUserAccessScope();

        if (!accessScope.systemAdmin()
                && workspaceId != null
                && !accessScope.workspaceIds().contains(workspaceId)) {
            throw new AccessDeniedException("Không có quyền xem người dùng của workspace này");
        }
        if (teamId != null) {
            Long teamWorkspaceId = teamRepository.findById(teamId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "Không tìm thấy nhóm làm việc"))
                    .getWorkspace()
                    .getId();
            if (!accessScope.systemAdmin() && !accessScope.workspaceIds().contains(teamWorkspaceId)) {
                throw new AccessDeniedException("Không có quyền xem người dùng của nhóm này");
            }
        }

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
        Specification<User> spec = buildSearchSpec(
                keyword,
                status,
                workspaceId,
                roleId,
                teamId,
                accessScope.systemAdmin() ? null : accessScope.workspaceIds()
        );

        Page<User> result = userRepository.findAll(spec, pageable);

        return PageResponse.<UserResponse>builder()
                .items(result.getContent().stream().map(userMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // chưa test
    /** UC-1.6 — Xem chi tiết tài khoản */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:read')")
    public UserResponse findById(Long id) {
        User user = getUser(id);
        UserAccessScope accessScope = resolveUserAccessScope();
        if (!accessScope.systemAdmin() && !isUserInWorkspaceScope(user.getId(), accessScope.workspaceIds())) {
            throw new AccessDeniedException("Không có quyền xem tài khoản này");
        }
        return toDetailResponse(user);
    }

    // chưa test
    /** UC-1.7 — Cập nhật thông tin tài khoản */
    @Transactional
    @PreAuthorize("hasAuthority('user:update')")
    public UserResponse update(Long id, UpdateUserRequest request) {
        if (id == null || id <= 0 || request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu cập nhật tài khoản không hợp lệ");
        }

        User actor = getCurrentUser();
        if (actor.getId().equals(id)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Vui lòng sử dụng API hồ sơ cá nhân để cập nhật chính mình"
            );
        }
        User user = getUser(id);
        boolean systemAdmin = isSystemAdmin(actor);
        if (!systemAdmin) {
            UserAccessScope actorScope = resolveUserAccessScope();
            if (!isUserInWorkspaceScope(id, actorScope.workspaceIds())) {
                throw new AccessDeniedException("Không có quyền cập nhật tài khoản ngoài workspace quản lý");
            }
            if (user.getRole() != null && user.getRole().getScope() == RoleScope.SYSTEM) {
                throw new AccessDeniedException("Workspace Owner không được cập nhật tài khoản hệ thống");
            }
        }

        List<String> changedFields = new ArrayList<>();

        if (StringUtils.hasText(request.getFullName())) {
            String fullName = request.getFullName().trim();
            if (!Objects.equals(user.getFullName(), fullName)) {
                user.setFullName(fullName);
                changedFields.add("fullName");
            }
        }
        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim().toLowerCase();
            if (!Objects.equals(user.getEmail(), email)) {
                validateUniqueEmail(email, id);
                user.setEmail(email);
                changedFields.add("email");
            }
        }
        if (StringUtils.hasText(request.getUsername())) {
            String username = request.getUsername().trim().toLowerCase();
            if (!Objects.equals(user.getUsername(), username)) {
                validateUniqueUsername(username, id);
                user.setUsername(username);
                changedFields.add("username");
            }
        }
        if (request.getPhone() != null) {
            String phone = normalizeOptionalValue(request.getPhone());
            validatePhone(phone);
            if (!Objects.equals(user.getPhone(), phone)) {
                user.setPhone(phone);
                changedFields.add("phone");
            }
        }
        if (request.getAvatarUrl() != null) {
            String avatarUrl = normalizeOptionalValue(request.getAvatarUrl());
            validateAvatarUrl(avatarUrl);
            if (!Objects.equals(user.getAvatarUrl(), avatarUrl)) {
                user.setAvatarUrl(avatarUrl);
                changedFields.add("avatarUrl");
            }
        }
        if (request.getDescription() != null) {
            String description = normalizeOptionalValue(request.getDescription());
            if (!Objects.equals(user.getDescription(), description)) {
                user.setDescription(description);
                changedFields.add("description");
            }
        }

        if (changedFields.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không có thông tin nào thay đổi");
        }

        User savedUser = userRepository.save(user);
        activityLogService.recordOrgEvent(
                actor.getId(),
                ActivityLogAction.USER_UPDATED,
                ActivityLogAction.TARGET_USER,
                user.getId(),
                String.join(",", changedFields)
        );
        return toDetailResponse(savedUser);
    }

    // chưa test
    /** UC-1.8 — Khoá / mở khoá tài khoản */
    @Transactional
    @PreAuthorize("hasAuthority('user:lock')")
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        validateStatusUpdate(id, request);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể thay đổi trạng thái tài khoản của chính mình");
        }

        User user = getUser(id);
        UserAccessScope accessScope = resolveUserAccessScope();
        if (!accessScope.systemAdmin() && !isUserInWorkspaceScope(id, accessScope.workspaceIds())) {
            throw new AccessDeniedException("Không có quyền thay đổi trạng thái tài khoản này");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể khóa/mở tài khoản đã bị xóa");
        }
        if (user.getStatus() == request.getStatus()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái tài khoản không thay đổi");
        }

        user.setStatus(request.getStatus());
        User savedUser = userRepository.save(user);

        String action;
        if (request.getStatus() == UserStatus.INACTIVE) {
            refreshTokenService.revokeAllForUser(id);
            action = ActivityLogAction.USER_LOCKED;
        } else {
            action = ActivityLogAction.USER_UNLOCKED;
        }
        activityLogService.recordOrgEvent(
                currentUserId,
                action,
                ActivityLogAction.TARGET_USER,
                id,
                user.getEmail()
        );

        return toDetailResponse(savedUser);
    }

    private void validateStatusUpdate(Long id, UpdateUserStatusRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "User id không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu trạng thái không được để trống");
        }
        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái không được để trống");
        }
        if (request.getStatus() != UserStatus.ACTIVE && request.getStatus() != UserStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Chỉ được khóa (INACTIVE) hoặc mở khóa (ACTIVE) tài khoản"
            );
        }
    }

//    /** UC-1.8 — Xoá mềm tài khoản (đặt trạng thái DELETED) */
//    @Transactional
//    @PreAuthorize("hasAuthority('user:update')")
//    public void delete(Long id) {
//        Long currentUserId = SecurityUtils.getCurrentUserId();
//        if (currentUserId.equals(id)) {
//            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể xoá tài khoản của chính mình");
//        }
//
//        User user = getUser(id);
//        if (user.getStatus() == UserStatus.DELETED) {
//            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
//        }
//
//        user.setStatus(UserStatus.DELETED);
//        userRepository.save(user);
//    }

    //chưa test
    /** UC-1.9 — Gán vai trò cho người dùng (SYSTEM hoặc WORKSPACE scope) */
    @Transactional
    @PreAuthorize("hasAuthority('user:assign-role')")
    public UserRoleResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        validateRoleUpdateRequest(userId, request);
        User actor = getCurrentUser();
        if (actor.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể tự thay đổi vai trò của chính mình");
        }

        User user = getUser(userId);
        Role role = roleService.getRole(request.getRoleId());
        validateAssignableRole(role);
        if (role.getScope() == RoleScope.WORKSPACE
                && (request.getWorkspaceId() == null || request.getWorkspaceId() <= 0)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "workspaceId là bắt buộc khi gán vai trò workspace"
            );
        }
        boolean systemAdmin = isSystemAdmin(actor);
        Long workspaceId = null;

        if (role.getScope() == RoleScope.SYSTEM) {
            if (!systemAdmin) {
                throw new AccessDeniedException("Chỉ System Admin mới được gán vai trò hệ thống");
            }
        } else {
            workspaceId = request.getWorkspaceId();
            Workspace workspace = workspaceService.getWorkspace(workspaceId);
            ensureWorkspaceActive(workspace);

            if (!systemAdmin) {
                UserAccessScope actorScope = resolveUserAccessScope();
                if (!actorScope.workspaceIds().contains(workspaceId)) {
                    throw new AccessDeniedException("Không có quyền gán vai trò trong workspace này");
                }
            }

            WorkspaceMember membership = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(workspaceId, userId)
                    .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Người dùng không thuộc workspace được phép quản lý"));
            membership.setRole(role);
            workspaceMemberRepository.save(membership);
        }

        user.setRole(role);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        activityLogService.recordOrgEvent(
                actor.getId(),
                ActivityLogAction.USER_ROLE_UPDATED,
                ActivityLogAction.TARGET_USER,
                user.getId(),
                role.getName() + (workspaceId != null ? "@workspace:" + workspaceId : "")
        );

        return UserRoleResponse.builder()
                .userId(user.getId())
                .workspaceId(workspaceId)
                .role(roleService.findById(role.getId()))
                .build();
    }

    private void validateRoleUpdateRequest(Long userId, UpdateUserRoleRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "User id không hợp lệ");
        }
        if (request == null || request.getRoleId() == null || request.getRoleId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vai trò không hợp lệ");
        }
    }

    private void validateAssignableRole(Role role) {
        if (role.getScope() != RoleScope.SYSTEM && role.getScope() != RoleScope.WORKSPACE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ có thể gán vai trò hệ thống hoặc workspace (SYSTEM/WORKSPACE scope)");
        }
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() != null && SYSTEM_ADMIN_ROLE_NAME.equals(user.getRole().getName());
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu tạo tài khoản không được để trống");
        }

        if (!StringUtils.hasText(request.getFullName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Họ tên không được để trống");
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email không được để trống");
        }

        if (request.getWorkspaceId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace không được để trống");
        }

        if (request.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể tạo tài khoản với trạng thái DELETED");
        }

        String username = StringUtils.hasText(request.getUsername())
                ? request.getUsername().trim().toLowerCase()
                : null;
        if (username != null && (username.length() < 3 || username.length() > 100)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Username từ 3–100 ký tự");
        }
        if (username != null && username.chars().noneMatch(Character::isLetter)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Username phải chứa ít nhất một chữ cái");
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!phone.isEmpty() && (phone.length() > 20 || !PHONE_PATTERN.matcher(phone).matches())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số điện thoại không hợp lệ");
            }
        }
    }

    private String resolveUsername(CreateUserRequest request, String employeeCode) {
        if (StringUtils.hasText(request.getUsername())) {
            return request.getUsername().trim().toLowerCase();
        }
        return "user" + employeeCode;
    }

    private String generateDefaultPassword(String username) {
        String normalizedUsername = username.replaceAll("[^A-Za-z0-9]", "");
        String capitalizedUsername = Character.toUpperCase(normalizedUsername.charAt(0))
                + normalizedUsername.substring(1);
        return capitalizedUsername + "@123";
    }

    private Role resolveUserRole(CreateUserRequest request) {
        Role resolvedRole = resolveRequestedRole(request);
        if (resolvedRole.getScope() == RoleScope.SYSTEM || resolvedRole.getScope() == RoleScope.WORKSPACE) {
            validateAssignableRole(resolvedRole);
            return resolvedRole;
        }
        return getDefaultUserRole();
    }

    private Role resolveWorkspaceMemberRole(Role userRole) {
        if (userRole.getScope() == RoleScope.WORKSPACE) {
            return userRole;
        }
        return getDefaultUserRole();
    }

    private Role resolveRequestedRole(CreateUserRequest request) {
        if (request.getRoleId() != null) {
            return roleService.getRole(request.getRoleId());
        }

        if (StringUtils.hasText(request.getRole())) {
            return findRoleByDisplayName(toRoleDisplayName(request.getRole()));
        }

        return getDefaultUserRole();
    }

    private String toRoleDisplayName(String roleKey) {
        return Arrays.stream(roleKey.trim().split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private Role findRoleByDisplayName(String roleName) {
        return roleRepository.findFirstByName(roleName)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò"));
    }

    private Role getDefaultUserRole() {
        return roleRepository.findByNameAndScope(DEFAULT_USER_ROLE_NAME, RoleScope.WORKSPACE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Không tìm thấy vai trò mặc định: " + DEFAULT_USER_ROLE_NAME
                ));
    }

    private void ensureWorkspaceActive(Workspace workspace) {
        if (workspace.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã đóng");
        }
    }

    private void ensureTeamInWorkspace(Long teamId, Long workspaceId) {
        teamRepository.findByIdAndWorkspaceId(teamId, workspaceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Phòng ban / nhóm không thuộc workspace đã chọn"
                ));
    }

    private void assignToTeam(Long workspaceId, Long teamId, WorkspaceMember workspaceMember) {
        Role teamRole = roleRepository.findByNameAndScope(DEFAULT_TEAM_MEMBER_ROLE_NAME, RoleScope.TEAM)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Không tìm thấy vai trò mặc định: " + DEFAULT_TEAM_MEMBER_ROLE_NAME
                ));

        AddTeamMemberRequest addRequest = new AddTeamMemberRequest();
        addRequest.setWorkspaceMemberId(workspaceMember.getId());
        addRequest.setRoleId(teamRole.getId());
        teamMemberService.add(workspaceId, teamId, addRequest);
    }

    private User getCurrentUser() {
        User user = getUser(SecurityUtils.getCurrentUserId());
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản đã bị vô hiệu hóa");
        }
        return user;
    }

    private void validateProfile(UpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu hồ sơ không được để trống");
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!phone.isEmpty() && (phone.length() > 20 || !PHONE_PATTERN.matcher(phone).matches())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số điện thoại không hợp lệ");
            }
        }

        if (request.getAvatarUrl() != null) {
            String avatarUrl = request.getAvatarUrl().trim();
            if (avatarUrl.length() > 500
                    || (!avatarUrl.isEmpty() && !avatarUrl.matches("(?i)^https?://[^\\s]+$"))) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Avatar URL không hợp lệ");
            }
        }

        boolean hasPasswordInput = StringUtils.hasText(request.getCurrentPassword())
                || StringUtils.hasText(request.getNewPassword())
                || StringUtils.hasText(request.getConfirmNewPassword());
        if (hasPasswordInput && !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng nhập mật khẩu mới");
        }
    }

    private void validatePhone(String phone) {
        if (phone != null && (phone.length() > 20 || !PHONE_PATTERN.matcher(phone).matches())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số điện thoại không hợp lệ");
        }
    }

    private void validateAvatarUrl(String avatarUrl) {
        if (avatarUrl != null
                && (avatarUrl.length() > 500 || !avatarUrl.matches("(?i)^https?://[^\\s]+$"))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Avatar URL không hợp lệ");
        }
    }

    private void validatePasswordPolicy(String password) {
        if (password.length() < 8
                || !UPPERCASE_PATTERN.matcher(password).matches()
                || !LOWERCASE_PATTERN.matcher(password).matches()
                || !DIGIT_PATTERN.matcher(password).matches()
                || !SPECIAL_CHARACTER_PATTERN.matcher(password).matches()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
            );
        }
    }

    private String normalizeOptionalValue(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private UserResponse toDetailResponse(User user) {
        return userMapper.toResponse(user, loadPermissions(user));
    }

    private List<Permission> loadPermissions(User user) {
        if (user.getRole() == null) {
            return List.of();
        }
        return rolePermissionRepository.findPermissionsByRoleId(user.getRole().getId());
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        boolean exists = excludeId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được đăng ký");
        }
    }

    private void validateUniqueUsername(String username, Long excludeId) {
        boolean exists = excludeId == null
                ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username đã được sử dụng");
        }
    }

    private Specification<User> buildSearchSpec(
            String keyword,
            UserStatus status,
            Long workspaceId,
            Long roleId,
            Long teamId,
            Set<Long> scopedWorkspaceIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("employeeCode")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), UserStatus.DELETED));
            }

            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("id"), roleId));
            }

            if (workspaceId != null) {
                predicates.add(userBelongsToWorkspaces(root, query, cb, Set.of(workspaceId)));
            }

            if (scopedWorkspaceIds != null) {
                if (scopedWorkspaceIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(userBelongsToWorkspaces(root, query, cb, scopedWorkspaceIds));
                }
            }

            if (teamId != null) {
                Subquery<Long> teamMemberSubquery = query.subquery(Long.class);
                Root<TeamMember> teamMember = teamMemberSubquery.from(TeamMember.class);
                teamMemberSubquery.select(teamMember.get("workspaceMember").get("user").get("id"))
                        .where(cb.and(
                                cb.equal(teamMember.get("workspaceMember").get("user").get("id"), root.get("id")),
                                cb.equal(teamMember.get("team").get("id"), teamId),
                                cb.equal(teamMember.get("status"), MemberStatus.ACTIVE),
                                cb.equal(teamMember.get("workspaceMember").get("status"), MemberStatus.ACTIVE)
                        ));
                predicates.add(cb.exists(teamMemberSubquery));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate userBelongsToWorkspaces(
            Root<User> userRoot,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Set<Long> workspaceIds
    ) {
        Subquery<Long> memberSubquery = query.subquery(Long.class);
        Root<WorkspaceMember> member = memberSubquery.from(WorkspaceMember.class);
        memberSubquery.select(member.get("user").get("id"))
                .where(cb.and(
                        cb.equal(member.get("user").get("id"), userRoot.get("id")),
                        member.get("workspace").get("id").in(workspaceIds),
                        cb.equal(member.get("status"), MemberStatus.ACTIVE)
                ));

        Subquery<Long> ownerSubquery = query.subquery(Long.class);
        Root<Workspace> ownedWorkspace = ownerSubquery.from(Workspace.class);
        ownerSubquery.select(ownedWorkspace.get("owner").get("id"))
                .where(cb.and(
                        cb.equal(ownedWorkspace.get("owner").get("id"), userRoot.get("id")),
                        ownedWorkspace.get("id").in(workspaceIds)
                ));

        return cb.or(cb.exists(memberSubquery), cb.exists(ownerSubquery));
    }

    private UserAccessScope resolveUserAccessScope() {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (SYSTEM_ADMIN_ROLE_NAME.equals(roleName)) {
            return new UserAccessScope(true, Set.of());
        }
        if (!WORKSPACE_OWNER_ROLE_NAME.equals(roleName)) {
            throw new AccessDeniedException("Không có quyền xem tài khoản người dùng");
        }

        Set<Long> workspaceIds = new LinkedHashSet<>(workspaceRepository.findIdsByOwnerId(currentUser.getId()));
        workspaceIds.addAll(workspaceMemberRepository.findOwnedWorkspaceIds(
                currentUser.getId(), MemberStatus.ACTIVE));
        return new UserAccessScope(false, Set.copyOf(workspaceIds));
    }

    private boolean isUserInWorkspaceScope(Long userId, Set<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) {
            return false;
        }
        return workspaceMemberRepository.existsByWorkspaceIdInAndUserIdAndStatus(
                workspaceIds, userId, MemberStatus.ACTIVE)
                || workspaceRepository.existsByIdInAndOwner_Id(workspaceIds, userId);
    }

    private String validateSortField(String sortBy) {
        String resolved = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
        if (!USER_SORT_FIELDS.contains(resolved)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trường sắp xếp không hợp lệ");
        }
        return resolved;
    }

    private Sort.Direction parseSortDirection(String sortDirection) {
        if (!StringUtils.hasText(sortDirection) || "desc".equalsIgnoreCase(sortDirection.trim())) {
            return Sort.Direction.DESC;
        }
        if ("asc".equalsIgnoreCase(sortDirection.trim())) {
            return Sort.Direction.ASC;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chiều sắp xếp phải là asc hoặc desc");
    }

    private record UserAccessScope(boolean systemAdmin, Set<Long> workspaceIds) {
    }

}
