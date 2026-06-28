package com.workmanagement.backend.attachment.mapper;

import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.entity.Attachment;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {

    public AttachmentResponse toResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .projectId(attachment.getProject() != null ? attachment.getProject().getId() : null)
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .uploadedByMemberId(attachment.getUploadedByMember().getId())
                .uploadedByName(attachment.getUploadedByMember()
                        .getTeamMember()
                        .getWorkspaceMember()
                        .getUser()
                        .getFullName())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

}
