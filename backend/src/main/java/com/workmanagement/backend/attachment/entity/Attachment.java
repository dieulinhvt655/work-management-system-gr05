package com.workmanagement.backend.attachment.entity;

import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_member_id", nullable = false)
    private ProjectMember uploadedByMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

}
