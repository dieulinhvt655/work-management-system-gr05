package com.workmanagement.backend.task.repository;

import com.workmanagement.backend.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByPbiIdOrderByCreatedAtDesc(Long pbiId);

    Optional<Task> findByIdAndPbiId(Long id, Long pbiId);

    List<Task> findBySprintIdOrderByCreatedAtDesc(Long sprintId);

    Optional<Task> findByIdAndSprintId(Long id, Long sprintId);

    boolean existsByParentTaskId(Long parentTaskId);

    long countBySprintId(Long sprintId);

    List<Task> findBySprintIdAndPbiId(Long sprintId, Long pbiId);

    Optional<Task> findByIdAndPbi_Backlog_Project_Id(Long id, Long projectId);

}
