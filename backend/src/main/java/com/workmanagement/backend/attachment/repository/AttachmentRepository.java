package com.workmanagement.backend.attachment.repository;

import com.workmanagement.backend.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByProjectIdAndCommentIdIsNullOrderByUploadedAtDesc(Long projectId);

    Optional<Attachment> findByIdAndProjectIdAndCommentIdIsNull(Long id, Long projectId);

    List<Attachment> findByCommentIdOrderByUploadedAtAsc(Long commentId);

    List<Attachment> findByCommentIdInOrderByUploadedAtAsc(Collection<Long> commentIds);

    Optional<Attachment> findByIdAndCommentId(Long id, Long commentId);

}
