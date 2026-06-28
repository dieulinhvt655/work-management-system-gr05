package com.workmanagement.backend.comment.dto.response;

import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.common.enums.CommentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long taskId;
    private Long parentCommentId;
    private String content;
    private CommentStatus status;
    private Long authorMemberId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentResponse> attachments;

}
