package com.workmanagement.backend.comment.mapper;

import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.comment.dto.response.CommentResponse;
import com.workmanagement.backend.comment.entity.Comment;
import com.workmanagement.backend.project.entity.ProjectMember;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        return toResponse(comment, List.of());
    }

    public CommentResponse toResponse(Comment comment, List<AttachmentResponse> attachments) {
        ProjectMember author = comment.getAuthorMember();
        String authorName = null;
        Long authorMemberId = null;
        if (author != null) {
            authorMemberId = author.getId();
            authorName = author.getTeamMember().getWorkspaceMember().getUser().getFullName();
        }

        Long parentCommentId = comment.getParentComment() != null
                ? comment.getParentComment().getId()
                : null;

        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .parentCommentId(parentCommentId)
                .content(comment.getContent())
                .status(comment.getStatus())
                .authorMemberId(authorMemberId)
                .authorName(authorName)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .attachments(attachments)
                .build();
    }

}
