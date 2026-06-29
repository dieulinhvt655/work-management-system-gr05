package com.workmanagement.backend.attachment.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.attachment.dto.request.UpdateAttachmentRequest;
import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.entity.Attachment;
import com.workmanagement.backend.attachment.mapper.AttachmentMapper;
import com.workmanagement.backend.attachment.repository.AttachmentRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.comment.entity.Comment;
import com.workmanagement.backend.comment.repository.CommentRepository;
import com.workmanagement.backend.common.enums.CommentStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<CommentStatus> VISIBLE_COMMENT_STATUSES = Set.of(
            CommentStatus.ACTIVE, CommentStatus.EDITED
    );

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamService teamService;
    private final ActivityLogService activityLogService;
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;

    /** UC-3.9 — Danh sách tài liệu dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('attachment:read')")
    public List<AttachmentResponse> findByProject(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        return attachmentRepository.findByProjectIdAndCommentIdIsNullOrderByUploadedAtDesc(projectId)
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

    /** UC-6.3 — Danh sách tệp đính kèm bình luận */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('attachment:read')")
    public List<AttachmentResponse> findByComment(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getVisibleComment(projectId, taskId, commentId);

        return attachmentRepository.findByCommentIdOrderByUploadedAtAsc(commentId)
                .stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    /** UC-6.3 — Đính kèm tệp vào bình luận */
    @Transactional
    @PreAuthorize("hasAnyAuthority('attachment:create', 'comment:create')")
    public AttachmentResponse uploadToComment(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId,
            MultipartFile file
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        ensureProjectAcceptsDocuments(project);

        getVisibleComment(projectId, taskId, commentId);
        ProjectMember uploader = resolveUploaderMembership(project);

        FileStorageService.StoredFile storedFile = fileStorageService.storeCommentFile(
                projectId, taskId, commentId, file
        );
        Attachment attachment = Attachment.builder()
                .uploadedByMember(uploader)
                .project(project)
                .taskId(taskId)
                .commentId(commentId)
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

    /** UC-6.3 — Xóa tệp đính kèm bình luận */
    @Transactional
    @PreAuthorize("hasAnyAuthority('attachment:delete', 'comment:update')")
    public void deleteCommentAttachment(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId,
            Long attachmentId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        Comment comment = getVisibleComment(projectId, taskId, commentId);
        Attachment attachment = getCommentAttachment(commentId, attachmentId);
        verifyCanDeleteCommentAttachment(project, comment, attachment);

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

    /** UC-6.3 — Tải tệp đính kèm bình luận */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('attachment:read')")
    public DownloadableAttachment downloadCommentAttachment(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId,
            Long attachmentId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getVisibleComment(projectId, taskId, commentId);

        Attachment attachment = getCommentAttachment(commentId, attachmentId);
        Resource resource = fileStorageService.loadAsResource(attachment.getFileUrl());
        String contentType = StringUtils.hasText(attachment.getFileType())
                ? attachment.getFileType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return new DownloadableAttachment(resource, attachment.getFileName(), contentType);
    }

    /** Hỗ trợ UC-6.4 — Nhóm tệp đính kèm theo danh sách bình luận */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> findResponsesByCommentIds(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return List.of();
        }
        return attachmentRepository.findByCommentIdInOrderByUploadedAtAsc(commentIds)
                .stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    private Attachment getProjectAttachment(Long projectId, Long attachmentId) {
        return attachmentRepository.findByIdAndProjectIdAndCommentIdIsNull(attachmentId, projectId)
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

    private Task getTask(Long projectId, Long taskId) {
        return taskRepository.findByIdAndPbi_Backlog_Project_Id(taskId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy task"));
    }

    private Comment getVisibleComment(Long projectId, Long taskId, Long commentId) {
        getTask(projectId, taskId);

        Comment comment = commentRepository.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận"));
        if (!VISIBLE_COMMENT_STATUSES.contains(comment.getStatus())) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận");
        }
        return comment;
    }

    private Attachment getCommentAttachment(Long commentId, Long attachmentId) {
        return attachmentRepository.findByIdAndCommentId(attachmentId, commentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ATTACHMENT_NOT_FOUND,
                        "Không tìm thấy tệp đính kèm"
                ));
    }

    private void verifyCanDeleteCommentAttachment(Project project, Comment comment, Attachment attachment) {
        if (projectService.isActiveProjectManager(project)) {
            return;
        }
        if (isCommentAuthor(comment)) {
            return;
        }
        if (isAttachmentUploader(attachment)) {
            return;
        }
        throw new BusinessException(
                ErrorCode.ATTACHMENT_ACCESS_DENIED,
                "Không có quyền xóa tệp đính kèm"
        );
    }

    private boolean isCommentAuthor(Comment comment) {
        ProjectMember author = comment.getAuthorMember();
        return author != null
                && author.getStatus() == MemberStatus.ACTIVE
                && author.getTeamMember().getWorkspaceMember().getUser().getId()
                .equals(SecurityUtils.getCurrentUserId());
    }

    private boolean isAttachmentUploader(Attachment attachment) {
        ProjectMember uploader = attachment.getUploadedByMember();
        return uploader != null
                && uploader.getStatus() == MemberStatus.ACTIVE
                && uploader.getTeamMember().getWorkspaceMember().getUser().getId()
                .equals(SecurityUtils.getCurrentUserId());
    }

    public record DownloadableAttachment(Resource resource, String fileName, String contentType) {
    }

}
