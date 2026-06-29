package com.workmanagement.backend.task.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.task.dto.request.CreateWorkflowTransitionRequest;
import com.workmanagement.backend.task.dto.request.UpdateWorkflowTransitionRequest;
import com.workmanagement.backend.task.dto.response.WorkflowTransitionResponse;
import com.workmanagement.backend.task.entity.WorkflowState;
import com.workmanagement.backend.task.entity.WorkflowTransition;
import com.workmanagement.backend.task.mapper.WorkflowTransitionMapper;
import com.workmanagement.backend.task.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowTransitionService {

    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowTransitionMapper workflowTransitionMapper;
    private final WorkflowStateService workflowStateService;
    private final ProjectService projectService;

    /** UC-5.0 — Danh sách transition workflow của dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public List<WorkflowTransitionResponse> findAll(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        return workflowTransitionRepository.findByProjectId(projectId)
                .stream()
                .map(workflowTransitionMapper::toResponse)
                .toList();
    }

    /** UC-5.0 — Tạo transition chuyển trạng thái workflow */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public WorkflowTransitionResponse create(
            Long workspaceId,
            Long teamId,
            Long projectId,
            CreateWorkflowTransitionRequest request
    ) {
        Project project = getEditableProject(workspaceId, teamId, projectId);
        projectService.verifyCanUpdateProject(project);

        WorkflowState fromState = workflowStateService.getState(projectId, request.getFromStateId());
        WorkflowState toState = workflowStateService.getState(projectId, request.getToStateId());

        if (workflowTransitionRepository.findByProjectIdAndFromStateIdAndToStateId(
                projectId, fromState.getId(), toState.getId()
        ).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Transition đã tồn tại");
        }

        WorkflowTransition transition = WorkflowTransition.builder()
                .project(project)
                .fromState(fromState)
                .toState(toState)
                .name(request.getName())
                .build();

        return workflowTransitionMapper.toResponse(workflowTransitionRepository.save(transition));
    }

    /** UC-5.0 — Cập nhật transition workflow */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public WorkflowTransitionResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long transitionId,
            UpdateWorkflowTransitionRequest request
    ) {
        Project project = getEditableProject(workspaceId, teamId, projectId);
        projectService.verifyCanUpdateProject(project);

        WorkflowTransition transition = getTransition(projectId, transitionId);

        if (request.getFromStateId() != null) {
            transition.setFromState(workflowStateService.getState(projectId, request.getFromStateId()));
        }
        if (request.getToStateId() != null) {
            transition.setToState(workflowStateService.getState(projectId, request.getToStateId()));
        }
        if (request.getName() != null) {
            transition.setName(request.getName());
        }

        return workflowTransitionMapper.toResponse(workflowTransitionRepository.save(transition));
    }

    /** UC-5.0 — Xóa transition workflow */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public void delete(Long workspaceId, Long teamId, Long projectId, Long transitionId) {
        Project project = getEditableProject(workspaceId, teamId, projectId);
        projectService.verifyCanUpdateProject(project);

        WorkflowTransition transition = getTransition(projectId, transitionId);
        workflowTransitionRepository.delete(transition);
    }

    /** Hỗ trợ UC-5.5/5.7 — Kiểm tra transition hợp lệ khi chuyển trạng thái task */
    void validateTransition(Long projectId, Long fromStateId, Long toStateId) {
        workflowTransitionRepository.findByProjectIdAndFromStateIdAndToStateId(projectId, fromStateId, toStateId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKFLOW_TRANSITION_INVALID,
                        "Không được phép chuyển trạng thái này"
                ));
    }

    private WorkflowTransition getTransition(Long projectId, Long transitionId) {
        return workflowTransitionRepository.findByIdAndProjectId(transitionId, projectId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKFLOW_TRANSITION_NOT_FOUND,
                        "Không tìm thấy transition"
                ));
    }

    private Project getEditableProject(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
        return project;
    }

}
