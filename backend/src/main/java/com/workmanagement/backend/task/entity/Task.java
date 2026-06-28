package com.workmanagement.backend.task.entity;

import com.workmanagement.backend.common.enums.PriorityLevel;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
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
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pbi_id", nullable = false)
    private ProductBacklogItem pbi;

    @Column(name = "sprint_id")
    private Long sprintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_member_id")
    private ProjectMember assigneeMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_member_id", nullable = false)
    private ProjectMember reporterMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_member_id")
    private ProjectMember reviewerMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_state_id")
    private WorkflowState workflowState;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private PriorityLevel priority;

    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false)
    private Integer progress;

    @Column(name = "start_date")
    private LocalDate startDate;

    private LocalDate deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (priority == null) {
            priority = PriorityLevel.MEDIUM;
        }
        if (status == null) {
            status = TaskStatus.TO_DO;
        }
        if (progress == null) {
            progress = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPreparationTask() {
        return sprintId == null;
    }

}
