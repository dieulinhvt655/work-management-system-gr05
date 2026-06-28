package com.workmanagement.backend.attachment.repository;

import com.workmanagement.backend.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByProjectIdOrderByUploadedAtDesc(Long projectId);

    Optional<Attachment> findByIdAndProjectId(Long id, Long projectId);

}
