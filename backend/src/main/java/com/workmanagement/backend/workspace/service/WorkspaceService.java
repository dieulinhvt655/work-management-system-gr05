package com.workmanagement.backend.workspace.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
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
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final String WORKSPACE_OWNER_ROLE = "Workspace Owner";
    private static final String SYSTEM_ADMIN_ROLE = "System Admin";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMapper workspaceMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /** UC-2.1 — Tạo workspace và thêm owner làm thành viên */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:create')")
    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        User owner = getCurrentUser();

        Workspace workspace = Workspace.builder()
                .owner(owner)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(CommonStatus.ACTIVE)
                .build();
        workspace = workspaceRepository.save(workspace);

        Role workspaceOwnerRole = findWorkspaceRole(WORKSPACE_OWNER_ROLE);
        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(workspaceOwnerRole)
                .addedByOwner(owner)
                .status(MemberStatus.ACTIVE)
                .build());

        return workspaceMapper.toResponse(workspace);
    }

    /** UC-2.2 — Danh sách workspace user có quyền xem */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('workspace:read')")
    public PageResponse<WorkspaceResponse> findAll(int page, int size, String keyword, CommonStatus status) {
        Long userId = SecurityUtils.getCurrentUserId();
        User currentUser = getCurrentUser();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Workspace> spec = buildSearchSpec(keyword, status);
        if (!isSystemAdmin(currentUser)) {
            spec = spec.and(accessibleSpec(userId));
        }

        Page<Workspace> result = workspaceRepository.findAll(spec, pageable);

        return PageResponse.<WorkspaceResponse>builder()
                .items(result.getContent().stream().map(workspaceMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-2.2 — Chi tiết workspace */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('workspace:read')")
    public WorkspaceResponse findById(Long id) {
        Workspace workspace = getWorkspace(id);
        verifyAccess(workspace);
        return workspaceMapper.toResponse(workspace);
    }

    /** UC-2.2 — Cập nhật thông tin workspace */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:update')")
    public WorkspaceResponse update(Long id, UpdateWorkspaceRequest request) {
        Workspace workspace = getWorkspace(id);
        verifyCanManage(workspace);
        ensureActive(workspace);

        if (StringUtils.hasText(request.getName())) {
            workspace.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

        return workspaceMapper.toResponse(workspaceRepository.save(workspace));
    }

    /** UC-2.10 — Đóng / vô hiệu hoá workspace */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:close')")
    public WorkspaceResponse close(Long id) {
        Workspace workspace = getWorkspace(id);
        verifyCanManage(workspace);

        if (workspace.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã được đóng");
        }

        workspace.setStatus(CommonStatus.INACTIVE);
        return workspaceMapper.toResponse(workspaceRepository.save(workspace));
    }

    public Workspace getWorkspace(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "Không tìm thấy workspace"));
    }

    public void verifyAccess(Workspace workspace) {
        User currentUser = getCurrentUser();
        if (isSystemAdmin(currentUser)) {
            return;
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

    public void verifyCanManage(Workspace workspace) {
        User currentUser = getCurrentUser();
        if (isSystemAdmin(currentUser)) {
            return;
        }
        Long userId = currentUser.getId();
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), userId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .filter(m -> WORKSPACE_OWNER_ROLE.equals(m.getRole().getName()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_ACCESS_DENIED,
                        "Chỉ workspace owner mới được thao tác này"
                ));
    }

    private void ensureActive(Workspace workspace) {
        if (workspace.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã đóng, không thể cập nhật");
        }
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

    private Specification<Workspace> accessibleSpec(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> memberSubquery = query.subquery(Long.class);
            Root<WorkspaceMember> memberRoot = memberSubquery.from(WorkspaceMember.class);
            memberSubquery.select(memberRoot.get("workspace").get("id"))
                    .where(cb.and(
                            cb.equal(memberRoot.get("user").get("id"), userId),
                            cb.equal(memberRoot.get("status"), MemberStatus.ACTIVE)
                    ));

            return cb.or(
                    cb.equal(root.get("owner").get("id"), userId),
                    root.get("id").in(memberSubquery)
            );
        };
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
