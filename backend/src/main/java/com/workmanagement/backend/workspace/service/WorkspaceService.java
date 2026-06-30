package com.workmanagement.backend.workspace.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.workspace.dto.request.CreateWorkspaceRequest;
import com.workmanagement.backend.workspace.dto.request.UpdateWorkspaceRequest;
import com.workmanagement.backend.workspace.dto.response.WorkspaceResponse;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.mapper.WorkspaceMapper;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.repository.WorkspaceRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final String WORKSPACE_OWNER_ROLE = "Workspace Owner";
    private static final String TEAM_LEADER_ROLE = "Team Leader";
    private static final String PROJECT_MANAGER_ROLE = "Project Manager";
    private static final String TEAM_MEMBER_ROLE = "Team Member";
    private static final String SYSTEM_ADMIN_ROLE = "System Admin";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMapper workspaceMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /** UC-2.1 — System Admin tạo workspace và chỉ định owner */
    @Transactional
    @PreAuthorize("hasAuthority('system:admin') and hasAuthority('workspace:create')")
    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }

        User admin = getCurrentUser();
        if (!isSystemAdmin(admin)) {
            throw new AccessDeniedException("Chỉ System Admin mới được tạo workspace");
        }

        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên workspace không được để trống");
        }
        if (request.getOwnerId() == null || request.getOwnerId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace owner không được để trống");
        }

        String workspaceName = request.getName().trim();
        validateWorkspaceTextLengths(workspaceName, request.getDescription());
        if (workspaceRepository.existsByNameIgnoreCase(workspaceName)) {
            throw new BusinessException(ErrorCode.WORKSPACE_NAME_ALREADY_EXISTS, "Tên workspace đã tồn tại");
        }

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy workspace owner"));
        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Workspace owner không hoạt động");
        }
        if (workspaceMemberRepository.existsByUserId(owner.getId())) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS,
                    "Workspace owner đã thuộc một workspace khác"
            );
        }

        Workspace workspace = Workspace.builder()
                .owner(owner)
                .name(workspaceName)
                .description(request.getDescription())
                .status(CommonStatus.ACTIVE)
                .build();
        workspace = workspaceRepository.save(workspace);

        Role workspaceOwnerRole = findWorkspaceRole(WORKSPACE_OWNER_ROLE);
        ensureDefaultWorkspaceRolesExist();
        if (!isSystemAdmin(owner)) {
            owner.setRole(workspaceOwnerRole);
            userRepository.save(owner);
        }
        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(workspaceOwnerRole)
                .addedByOwner(admin)
                .status(MemberStatus.ACTIVE)
                .build());

        activityLogService.recordOrgEvent(
                admin.getId(),
                ActivityLogAction.WORKSPACE_CREATED,
                ActivityLogAction.TARGET_WORKSPACE,
                workspace.getId(),
                workspace.getName()
        );

        return workspaceMapper.toResponse(workspace);
    }

    /** UC-2.2 — Danh sách toàn bộ workspace dành cho System Admin */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('system:admin') and hasAuthority('workspace:read')")
    public PageResponse<WorkspaceResponse> findAll(int page, int size, String keyword, CommonStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Workspace> spec = buildSearchSpec(keyword, status);
        Page<Workspace> result = workspaceRepository.findAll(spec, pageable);

        return PageResponse.<WorkspaceResponse>builder()
                .items(result.getContent().stream().map(workspaceMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-2.2 — System Admin tra cứu chi tiết workspace từ danh sách quản trị */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('system:admin') and hasAuthority('workspace:read')")
    public WorkspaceResponse findById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        User actor = getCurrentUser();
        if (!isSystemAdmin(actor)) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Chỉ System Admin mới được tra cứu workspace theo id"
            );
        }
        Workspace workspace = getWorkspace(id);
        return workspaceMapper.toResponse(workspace);
    }

    /** UC-2.2 — Owner/member xem workspace duy nhất mà mình đang tham gia */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('workspace:read')")
    public WorkspaceResponse findCurrent() {
        User currentUser = getCurrentUser();
        if (isSystemAdmin(currentUser)) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "System Admin sử dụng API tra cứu workspace theo id"
            );
        }

        List<WorkspaceMember> memberships = workspaceMemberRepository
                .findByUserIdAndStatus(currentUser.getId(), MemberStatus.ACTIVE);
        if (memberships.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Người dùng chưa thuộc workspace nào"
            );
        }
        if (memberships.size() > 1) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Người dùng đang thuộc nhiều hơn một workspace"
            );
        }

        Workspace workspace = memberships.getFirst().getWorkspace();
        if (workspace.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Workspace đã ngừng hoạt động"
            );
        }

        return workspaceMapper.toResponse(workspace);
    }


    /** UC-2.2 — Cập nhật thông tin workspace */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:update')")
    public WorkspaceResponse update(Long id, UpdateWorkspaceRequest request) {
        validateUpdateRequest(id, request);

        Workspace workspace = getWorkspace(id);
        User actor = getCurrentUser();
        boolean systemAdmin = isSystemAdmin(actor);
        boolean workspaceOwner = workspace.getOwner().getId().equals(actor.getId());

        if (!systemAdmin && !workspaceOwner) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Chỉ System Admin hoặc workspace owner mới được cập nhật workspace"
            );
        }
        if (!systemAdmin && workspace.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Workspace owner không được cập nhật workspace đã ngừng hoạt động"
            );
        }

        if (request.getName() != null) {
            String workspaceName = request.getName().trim();
            if (!StringUtils.hasText(workspaceName)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên workspace không được để trống");
            }
            if (workspaceName.length() > MAX_NAME_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên workspace tối đa 255 ký tự");
            }
            if (workspaceRepository.existsByNameIgnoreCaseAndIdNot(workspaceName, id)) {
                throw new BusinessException(
                        ErrorCode.WORKSPACE_NAME_ALREADY_EXISTS,
                        "Tên workspace đã tồn tại"
                );
            }
            workspace.setName(workspaceName);
        }
        if (request.getDescription() != null) {
            if (request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mô tả tối đa 2000 ký tự");
            }
            workspace.setDescription(request.getDescription());
        }
        workspace = workspaceRepository.save(workspace);

        activityLogService.recordOrgEvent(
                actor.getId(),
                ActivityLogAction.WORKSPACE_UPDATED,
                ActivityLogAction.TARGET_WORKSPACE,
                workspace.getId(),
                workspace.getName()
        );

        return workspaceMapper.toResponse(workspace);
    }

    private void validateUpdateRequest(Long id, UpdateWorkspaceRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
    }

    /** UC-2.10 — Đóng / vô hiệu hoá workspace */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:close')")
    public WorkspaceResponse close(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        Workspace workspace = getWorkspace(id);
        verifyCanManage(workspace);

        if (workspace.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã được đóng");
        }

        workspace.setStatus(CommonStatus.INACTIVE);
        workspace = workspaceRepository.save(workspace);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.WORKSPACE_CLOSED,
                ActivityLogAction.TARGET_WORKSPACE,
                workspace.getId(),
                workspace.getName()
        );

        return workspaceMapper.toResponse(workspace);
    }

    /** UC-2.10 — Chỉ System Admin được kích hoạt lại workspace */
    @Transactional
    @PreAuthorize("hasAuthority('system:admin') and hasAuthority('workspace:update')")
    public WorkspaceResponse reactivate(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }

        User admin = getCurrentUser();
        if (!isSystemAdmin(admin)) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Chỉ System Admin mới được kích hoạt lại workspace"
            );
        }

        Workspace workspace = getWorkspace(id);
        if (workspace.getStatus() == CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đang hoạt động");
        }

        workspace.setStatus(CommonStatus.ACTIVE);
        workspace = workspaceRepository.save(workspace);

        activityLogService.recordOrgEvent(
                admin.getId(),
                ActivityLogAction.WORKSPACE_REACTIVATED,
                ActivityLogAction.TARGET_WORKSPACE,
                workspace.getId(),
                workspace.getName()
        );

        return workspaceMapper.toResponse(workspace);
    }


    /** Hỗ trợ UC-2.x — Nạp workspace theo id; quyền được kiểm tra tại nghiệp vụ gọi */
    public Workspace getWorkspace(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "Không tìm thấy workspace"));
    }

    /** Hỗ trợ UC-2.2 — Kiểm tra quyền truy cập workspace */
    public void verifyAccess(Workspace workspace) {
        User currentUser = getCurrentUser();
        if (isSystemAdmin(currentUser)) {
            return;
        }
        if (workspace.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_ACCESS_DENIED,
                    "Workspace đã ngừng hoạt động"
            );
        }
        Long userId = currentUser.getId();
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(), userId, MemberStatus.ACTIVE)) {
            return;
        }
        throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "Không có quyền truy cập workspace");
    }

    /** Hỗ trợ UC-2.1/2.6 — Chỉ owner được chỉ định hoặc System Admin được quản lý workspace */
    public void verifyCanManage(Workspace workspace) {
        User currentUser = getCurrentUser();
        if (isSystemAdmin(currentUser)) {
            return;
        }
        if (workspace.getOwner().getId().equals(currentUser.getId())) {
            return;
        }
        throw new BusinessException(
                ErrorCode.WORKSPACE_ACCESS_DENIED,
                "Chỉ System Admin hoặc workspace owner mới được thao tác này"
        );
    }

    private User getCurrentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() != null && SYSTEM_ADMIN_ROLE.equals(user.getRole().getName());
    }

    private Role findWorkspaceRole(String name) {
        return roleRepository.findByNameAndScope(name, RoleScope.WORKSPACE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò workspace"));
    }

    private void ensureDefaultWorkspaceRolesExist() {
        ensureRoleExists(TEAM_LEADER_ROLE, RoleScope.TEAM);
        ensureRoleExists(PROJECT_MANAGER_ROLE, RoleScope.PROJECT);
        ensureRoleExists(TEAM_MEMBER_ROLE, RoleScope.TEAM);
    }

    private void ensureRoleExists(String name, RoleScope scope) {
        if (!roleRepository.existsByNameAndScope(name, scope)) {
            throw new BusinessException(
                    ErrorCode.ROLE_NOT_FOUND,
                    "Không tìm thấy vai trò mặc định: " + name
            );
        }
    }

    private void validateWorkspaceTextLengths(String name, String description) {
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên workspace tối đa 255 ký tự");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mô tả tối đa 2000 ký tự");
        }
    }

    private Specification<Workspace> buildSearchSpec(String keyword, CommonStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
