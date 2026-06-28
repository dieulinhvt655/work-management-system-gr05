package com.workmanagement.backend.task.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.task.dto.request.CreateWorkflowStateRequest;
import com.workmanagement.backend.task.dto.request.UpdateWorkflowStateRequest;
import com.workmanagement.backend.task.dto.response.WorkflowStateResponse;
import com.workmanagement.backend.task.entity.WorkflowState;
import com.workmanagement.backend.task.mapper.WorkflowStateMapper;
import com.workmanagement.backend.task.repository.WorkflowStateRepository;
import com.workmanagement.backend.task.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowStateService {

    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowStateMapper workflowStateMapper;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public List<WorkflowStateResponse> findAll(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        return workflowStateRepository.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(workflowStateMapper::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public WorkflowStateResponse create(
            Long workspaceId,
            Long teamId,
            Long projectId,
            CreateWorkflowStateRequest request
    ) {
        Project project = getEditableProject(workspaceId, teamId, projectId);
        projectService.verifyCanUpdateProject(project);

        if (workflowStateRepository.findByProjectIdAndCode(projectId, request.getCode().trim()).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã trạng thái đã tồn tại");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultState(projectId);
        }

        WorkflowState state = WorkflowState.builder()
                .project(project)
                .name(request.getName().trim())
                .code(request.getCode().trim())
                .position(request.getPosition())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .isFinal(Boolean.TRUE.equals(request.getIsFinal()))
                .build();

        return workflowStateMapper.toResponse(workflowStateRepository.save(state));
    }

    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public WorkflowStateResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long stateId,
            UpdateWorkflowStateRequest request
    ) {
        Project project = getEditableProject(workspaceId, teamId, projectId);
        projectService.verifyCanUpdateProject(project);

        WorkflowState state = getState(projectId, stateId);

        if (StringUtils.hasText(request.getCode()) && !request.getCode().trim().equals(state.getCode())) {
            if (workflowStateRepository.findByProjectIdAndCode(projectId, request.getCode().trim()).isPresent()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã trạng thái đã tồn tại");
            }
            state.setCode(request.getCode().trim());
        }
        if (StringUtils.hasText(request.getName())) {
            state.setName(request.getName().trim());
        }
        if (request.getPosition() != null) {
            state.setPosition(request.getPosition());
        }
        if (request.getIsDefault() != null) {
            if (request.getIsDefault()) {
                clearDefaultState(projectId);
            }
            state.setIsDefault(request.getIsDefault());
        }
        if (request.getIsFinal() != null) {
            state.setIsFinal(request.getIsFinal());
        }

        return workflowStateMapper.toResponse(workflowStateRepository.save(state));
    }

    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public void delete(Long workspaceId, Long teamId, Long projectId, Long stateId) {
        Project project = getEditableProject(workspaceId, teamId, projectId);
        projectService.verifyCanUpdateProject(project);

        WorkflowState state = getState(projectId, stateId);
        workflowTransitionRepository.findByProjectId(projectId).stream()
                .filter(t -> t.getFromState().getId().equals(stateId) || t.getToState().getId().equals(stateId))
                .findAny()
                .ifPresent(t -> {
                    throw new BusinessException(ErrorCode.WORKFLOW_STATE_IN_USE, "Trạng thái đang được dùng trong transition");
                });

        workflowStateRepository.delete(state);
    }

    @Transactional
    public WorkflowState ensureDefaultWorkflow(Project project) {
        if (!workflowStateRepository.existsByProjectId(project.getId())) {
            seedDefaultWorkflow(project);
        }
        return workflowStateRepository.findByProjectIdAndIsDefaultTrue(project.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_STATE_NOT_FOUND, "Không tìm thấy trạng thái mặc định"));
    }

    WorkflowState getStateByCode(Long projectId, String code) {
        return workflowStateRepository.findByProjectIdAndCode(projectId, code)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_STATE_NOT_FOUND, "Không tìm thấy trạng thái workflow"));
    }

    WorkflowState getState(Long projectId, Long stateId) {
        return workflowStateRepository.findByIdAndProjectId(stateId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_STATE_NOT_FOUND, "Không tìm thấy trạng thái workflow"));
    }

    private void seedDefaultWorkflow(Project project) {
        WorkflowState toDo = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("To Do")
                .code("to_do")
                .position(0)
                .isDefault(true)
                .isFinal(false)
                .build());
        WorkflowState inProgress = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("In Progress")
                .code("in_progress")
                .position(1)
                .isDefault(false)
                .isFinal(false)
                .build());
        WorkflowState review = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("Review")
                .code("review")
                .position(2)
                .isDefault(false)
                .isFinal(false)
                .build());
        WorkflowState done = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("Done")
                .code("done")
                .position(3)
                .isDefault(false)
                .isFinal(true)
                .build());

        workflowTransitionRepository.saveAll(List.of(
                buildTransition(project, toDo, inProgress, "Bắt đầu"),
                buildTransition(project, inProgress, review, "Gửi duyệt"),
                buildTransition(project, review, done, "Phê duyệt"),
                buildTransition(project, review, inProgress, "Trả lại")
        ));
    }

    private com.workmanagement.backend.task.entity.WorkflowTransition buildTransition(
            Project project,
            WorkflowState from,
            WorkflowState to,
            String name
    ) {
        return com.workmanagement.backend.task.entity.WorkflowTransition.builder()
                .project(project)
                .fromState(from)
                .toState(to)
                .name(name)
                .build();
    }

    private void clearDefaultState(Long projectId) {
        workflowStateRepository.findByProjectIdAndIsDefaultTrue(projectId)
                .ifPresent(state -> {
                    state.setIsDefault(false);
                    workflowStateRepository.save(state);
                });
    }

    private Project getEditableProject(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
        return project;
    }

}
