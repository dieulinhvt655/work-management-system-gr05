package com.workmanagement.backend.comment.service;

import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.service.AttachmentService;
import com.workmanagement.backend.comment.dto.request.CreateCommentRequest;
import com.workmanagement.backend.comment.dto.request.UpdateCommentRequest;
import com.workmanagement.backend.comment.dto.response.CommentResponse;
import com.workmanagement.backend.comment.entity.Comment;
import com.workmanagement.backend.comment.mapper.CommentMapper;
import com.workmanagement.backend.comment.repository.CommentRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommentStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Set<CommentStatus> VISIBLE_STATUSES = Set.of(CommentStatus.ACTIVE, CommentStatus.EDITED);
    private static final Set<CommentStatus> MODIFIABLE_STATUSES = Set.of(CommentStatus.ACTIVE, CommentStatus.EDITED);

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final AttachmentService attachmentService;

    /** UC-6.1 — Tạo bình luận trao đổi */
    @Transactional
    @PreAuthorize("hasAuthority('comment:create')")
    public CommentResponse create(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            CreateCommentRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        Task task = getTask(projectId, taskId);
        ProjectMember author = resolveAuthorMembership(project);
        Comment parentComment = resolveParentComment(taskId, request.getParentCommentId());

        Comment comment = Comment.builder()
                .task(task)
                .authorMember(author)
                .parentComment(parentComment)
                .content(request.getContent().trim())
                .status(CommentStatus.ACTIVE)
                .build();

        comment = commentRepository.save(comment);
        return commentMapper.toResponse(comment);
    }

    /** UC-6.4 — Xem trao đổi trong task */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('comment:read')")
    public List<CommentResponse> findByTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getTask(projectId, taskId);

        List<Comment> comments = commentRepository
                .findByTaskIdAndStatusInOrderByCreatedAtAsc(taskId, List.copyOf(VISIBLE_STATUSES));
        Map<Long, List<AttachmentResponse>> attachmentsByCommentId = groupAttachmentsByCommentId(
                comments.stream().map(Comment::getId).toList()
        );

        return comments.stream()
                .map(comment -> commentMapper.toResponse(
                        comment,
                        attachmentsByCommentId.getOrDefault(comment.getId(), List.of())
                ))
                .toList();
    }

    /** UC-6.4 — Chi tiết bình luận */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('comment:read')")
    public CommentResponse findById(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getTask(projectId, taskId);

        Comment comment = getVisibleComment(taskId, commentId);
        List<AttachmentResponse> attachments = attachmentService.findResponsesByCommentIds(List.of(commentId))
                .stream()
                .filter(attachment -> commentId.equals(attachment.getCommentId()))
                .toList();
        return commentMapper.toResponse(comment, attachments);
    }

    /** UC-6.2 — Chỉnh sửa bình luận */
    @Transactional
    @PreAuthorize("hasAuthority('comment:update')")
    public CommentResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId,
            UpdateCommentRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        Comment comment = getModifiableComment(taskId, commentId);
        verifyCanModifyComment(project, comment);

        comment.setContent(request.getContent().trim());
        comment.setStatus(CommentStatus.EDITED);
        comment = commentRepository.save(comment);

        return commentMapper.toResponse(comment);
    }

    /** UC-6.2 — Xóa bình luận */
    @Transactional
    @PreAuthorize("hasAnyAuthority('comment:delete', 'comment:update')")
    public void delete(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long taskId,
            Long commentId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        Comment comment = getModifiableComment(taskId, commentId);
        verifyCanModifyComment(project, comment);

        comment.setStatus(CommentStatus.DELETED);
        comment.setContent("[Đã xóa]");
        commentRepository.save(comment);
    }

    private Map<Long, List<AttachmentResponse>> groupAttachmentsByCommentId(List<Long> commentIds) {
        return attachmentService.findResponsesByCommentIds(commentIds).stream()
                .collect(Collectors.groupingBy(AttachmentResponse::getCommentId));
    }

    private Task getTask(Long projectId, Long taskId) {
        return taskRepository.findByIdAndPbi_Backlog_Project_Id(taskId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy task"));
    }

    private Comment getVisibleComment(Long taskId, Long commentId) {
        Comment comment = commentRepository.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận"));
        if (!VISIBLE_STATUSES.contains(comment.getStatus())) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận");
        }
        return comment;
    }

    private Comment getModifiableComment(Long taskId, Long commentId) {
        Comment comment = commentRepository.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận"));
        if (!MODIFIABLE_STATUSES.contains(comment.getStatus())) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Bình luận không còn được chỉnh sửa");
        }
        return comment;
    }

    private Comment resolveParentComment(Long taskId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }
        return getVisibleComment(taskId, parentCommentId);
    }

    private ProjectMember resolveAuthorMembership(Project project) {
        return projectMemberRepository
                .findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                        project.getId(),
                        SecurityUtils.getCurrentUserId(),
                        MemberStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PROJECT_MEMBER_NOT_FOUND,
                        "Bạn cần là thành viên dự án để bình luận"
                ));
    }

    private void verifyCanModifyComment(Project project, Comment comment) {
        if (projectService.isActiveProjectManager(project)) {
            return;
        }
        if (isCommentAuthor(comment)) {
            return;
        }
        throw new BusinessException(
                ErrorCode.COMMENT_ACCESS_DENIED,
                "Chỉ tác giả hoặc Project Manager mới được chỉnh sửa bình luận"
        );
    }

    private boolean isCommentAuthor(Comment comment) {
        ProjectMember author = comment.getAuthorMember();
        return author != null
                && author.getStatus() == MemberStatus.ACTIVE
                && author.getTeamMember().getWorkspaceMember().getUser().getId()
                .equals(SecurityUtils.getCurrentUserId());
    }

}
