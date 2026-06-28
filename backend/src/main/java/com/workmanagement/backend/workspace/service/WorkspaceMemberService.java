package com.workmanagement.backend.workspace.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.workspace.dto.request.AddWorkspaceMemberRequest;
import com.workmanagement.backend.workspace.dto.request.UpdateWorkspaceMemberRequest;
import com.workmanagement.backend.workspace.dto.response.WorkspaceMemberResponse;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.mapper.WorkspaceMemberMapper;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final ActivityLogService activityLogService;

    /** UC-2.6 — Danh sách thành viên workspace */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('workspace:read')")
    public List<WorkspaceMemberResponse> findAll(Long workspaceId) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyAccess(workspace);

        return workspaceMemberRepository.findByWorkspaceIdAndStatus(workspaceId, MemberStatus.ACTIVE)
                .stream()
                .map(workspaceMemberMapper::toResponse)
                .toList();
    }

    /** UC-2.6 — Thêm thành viên vào workspace */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:update')")
    public WorkspaceMemberResponse add(Long workspaceId, AddWorkspaceMemberRequest request) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyCanManage(workspace);
        ensureWorkspaceActive(workspace);

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getUserId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS, "User đã là thành viên workspace");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản không hoạt động");
        }

        Role role = roleService.getRole(request.getRoleId());
        validateWorkspaceRole(role);

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(role)
                .addedByOwner(currentUser)
                .status(MemberStatus.ACTIVE)
                .build();
        member = workspaceMemberRepository.save(member);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.WORKSPACE_MEMBER_ADDED,
                ActivityLogAction.TARGET_WORKSPACE_MEMBER,
                member.getId(),
                user.getFullName()
        );

        return workspaceMemberMapper.toResponse(member);
    }

    /** UC-2.6 — Cập nhật thông tin tổ chức thành viên (vai trò, trạng thái) */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:update')")
    public WorkspaceMemberResponse update(Long workspaceId, Long memberId, UpdateWorkspaceMemberRequest request) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyCanManage(workspace);
        ensureWorkspaceActive(workspace);

        WorkspaceMember member = workspaceMemberRepository.findById(memberId)
                .filter(m -> m.getWorkspace().getId().equals(workspaceId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
                        "Không tìm thấy thành viên workspace"
                ));

        if (workspace.getOwner().getId().equals(member.getUser().getId())
                && request.getStatus() == MemberStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể vô hiệu hoá owner workspace");
        }

        Role role = roleService.getRole(request.getRoleId());
        validateWorkspaceRole(role);
        member.setRole(role);

        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
            if (request.getStatus() == MemberStatus.INACTIVE) {
                member.setRemovedAt(LocalDateTime.now());
            } else {
                member.setRemovedAt(null);
            }
        }

        member = workspaceMemberRepository.save(member);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.WORKSPACE_MEMBER_UPDATED,
                ActivityLogAction.TARGET_WORKSPACE_MEMBER,
                member.getId(),
                role.getName()
        );

        return workspaceMemberMapper.toResponse(member);
    }

    private void validateWorkspaceRole(Role role) {
        if (role.getScope() != RoleScope.WORKSPACE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ được gán vai trò WORKSPACE scope");
        }
    }

    private void ensureWorkspaceActive(Workspace workspace) {
        if (workspace.getStatus() != com.workmanagement.backend.common.enums.CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã đóng");
        }
    }

}
