package com.workmanagement.backend.comment.repository;

import com.workmanagement.backend.comment.entity.Comment;
import com.workmanagement.backend.common.enums.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTaskIdAndStatusInOrderByCreatedAtAsc(Long taskId, List<CommentStatus> statuses);

    Optional<Comment> findByIdAndTaskId(Long id, Long taskId);

}
