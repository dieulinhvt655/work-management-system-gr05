package com.workmanagement.backend.productbacklog.entity;

import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.PbiType;
import com.workmanagement.backend.common.enums.PriorityLevel;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_backlog_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBacklogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "backlog_id", nullable = false)
    private ProductBacklog backlog;

    @Column(name = "sprint_id")
    private Long sprintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_member_id")
    private ProjectMember proposerMember;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private PbiType type;

    @Column(nullable = false)
    private PriorityLevel priority;

    @Column(nullable = false)
    private PbiStatus status;

    @Column(name = "desired_due_date")
    private LocalDate desiredDueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (type == null) {
            type = PbiType.FEATURE;
        }
        if (priority == null) {
            priority = PriorityLevel.MEDIUM;
        }
        if (status == null) {
            status = PbiStatus.NEW;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
