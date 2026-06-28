package com.workmanagement.backend.attachment.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.attachment.dto.request.UpdateAttachmentRequest;
import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.entity.Attachment;
import com.workmanagement.backend.attachment.mapper.AttachmentMapper;
import com.workmanagement.backend.attachment.repository.AttachmentRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamService teamService;
    private final ActivityLogService activityLogService;

    /** UC-3.9 — Danh sách tài liệu dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('attachment:read')")
    public List<AttachmentResponse> findByProject(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        return attachmentRepository.findByProjectIdOrderByUploadedAtDesc(projectId)
                .stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    /** UC-3.8 — Upload tài liệu dự án */
    @Transactional
    @PreAuthorize("hasAuthority('attachment:create')")
    public AttachmentResponse upload(
            Long workspaceId,
            Long teamId,
            Long projectId,
            MultipartFile file
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyCanManageProjectDocuments(project);
        ensureProjectAcceptsDocuments(project);

        ProjectMember uploader = resolveUploaderMembership(project);

        FileStorageService.StoredFile storedFile = fileStorageService.storeProjectFile(projectId, file);
        Attachment attachment = Attachment.builder()
                .uploadedByMember(uploader)
                .project(project)
                .fileName(storedFile.originalName())
                .fileType(storedFile.contentType())
                .fileSize(storedFile.size())
                .fileUrl(storedFile.relativePath())
                .build();
        attachment = attachmentRepository.save(attachment);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.ATTACHMENT_UPLOADED,
                ActivityLogAction.TARGET_ATTACHMENT,
                attachment.getId(),
                attachment.getFileName(),
                project
        );

        return attachmentMapper.toResponse(attachment);
    }

    /** UC-3.8 — Cập nhật tên tài liệu */
    @Transactional
    @PreAuthorize("hasAuthority('attachment:create')")
    public AttachmentResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long attachmentId,
            UpdateAttachmentRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyCanManageProjectDocuments(project);
        ensureProjectAcceptsDocuments(project);

        Attachment attachment = getProjectAttachment(projectId, attachmentId);
        attachment.setFileName(request.getFileName().trim());
        attachment = attachmentRepository.save(attachment);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.ATTACHMENT_UPDATED,
                ActivityLogAction.TARGET_ATTACHMENT,
                attachment.getId(),
                attachment.getFileName(),
                project
        );

        return attachmentMapper.toResponse(attachment);
    }

    /** UC-3.8 — Xóa tài liệu dự án */
    @Transactional
    @PreAuthorize("hasAuthority('attachment:delete')")
    public void delete(Long workspaceId, Long teamId, Long projectId, Long attachmentId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyCanManageProjectDocuments(project);
        ensureProjectAcceptsDocuments(project);

        Attachment attachment = getProjectAttachment(projectId, attachmentId);
        fileStorageService.delete(attachment.getFileUrl());
        attachmentRepository.delete(attachment);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.ATTACHMENT_DELETED,
                ActivityLogAction.TARGET_ATTACHMENT,
                attachmentId,
                attachment.getFileName(),
                project
        );
    }

    /** UC-3.9 — Tải tài liệu dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('attachment:read')")
    public DownloadableAttachment download(Long workspaceId, Long teamId, Long projectId, Long attachmentId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        Attachment attachment = getProjectAttachment(projectId, attachmentId);
        Resource resource = fileStorageService.loadAsResource(attachment.getFileUrl());
        String contentType = StringUtils.hasText(attachment.getFileType())
                ? attachment.getFileType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return new DownloadableAttachment(resource, attachment.getFileName(), contentType);
    }

    private Attachment getProjectAttachment(Long projectId, Long attachmentId) {
        return attachmentRepository.findByIdAndProjectId(attachmentId, projectId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ATTACHMENT_NOT_FOUND,
                        "Không tìm thấy tài liệu"
                ));
    }

    private void verifyCanManageProjectDocuments(Project project) {
        try {
            teamService.verifyCanManageProject(project.getTeam());
        } catch (BusinessException ex) {
            projectService.verifyCanUpdateProject(project);
        }
    }

    private void ensureProjectAcceptsDocuments(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

    private ProjectMember resolveUploaderMembership(Project project) {
        return projectMemberRepository
                .findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                        project.getId(),
                        SecurityUtils.getCurrentUserId(),
                        MemberStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Bạn cần là thành viên dự án để upload tài liệu"
                ));
    }

    public record DownloadableAttachment(Resource resource, String fileName, String contentType) {
    }

}
