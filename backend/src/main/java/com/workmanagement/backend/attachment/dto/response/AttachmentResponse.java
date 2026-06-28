package com.workmanagement.backend.attachment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttachmentResponse {

    private Long id;
    private Long projectId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Long uploadedByMemberId;
    private String uploadedByName;
    private LocalDateTime uploadedAt;

}
