package com.workmanagement.backend.attachment.service;

import com.workmanagement.backend.attachment.config.AttachmentProperties;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AttachmentProperties attachmentProperties;

    /** Hỗ trợ UC-3.8 — Lưu tệp tài liệu dự án lên đĩa */
    public StoredFile storeProjectFile(Long projectId, MultipartFile file) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        if (originalName.contains("..")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên tệp không hợp lệ");
        }

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetDir = Paths.get(attachmentProperties.getUploadDir(), "projects", projectId.toString());
        Path targetPath = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Không thể lưu tệp");
        }

        String relativePath = Paths.get("projects", projectId.toString(), storedName).toString();
        return new StoredFile(relativePath, originalName, file.getContentType(), file.getSize());
    }

    /** Hỗ trợ UC-6.3 — Lưu tệp đính kèm bình luận lên đĩa */
    public StoredFile storeCommentFile(Long projectId, Long taskId, Long commentId, MultipartFile file) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        if (originalName.contains("..")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên tệp không hợp lệ");
        }

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetDir = Paths.get(
                attachmentProperties.getUploadDir(),
                "projects",
                projectId.toString(),
                "tasks",
                taskId.toString(),
                "comments",
                commentId.toString()
        );
        Path targetPath = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Không thể lưu tệp");
        }

        String relativePath = Paths.get(
                "projects",
                projectId.toString(),
                "tasks",
                taskId.toString(),
                "comments",
                commentId.toString(),
                storedName
        ).toString();
        return new StoredFile(relativePath, originalName, file.getContentType(), file.getSize());
    }

    /** Hỗ trợ UC-3.9, UC-6.3 — Đọc tệp để tải xuống */
    public Resource loadAsResource(String relativePath) {
        try {
            Path filePath = Paths.get(attachmentProperties.getUploadDir()).resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND, "Không tìm thấy tệp");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND, "Không tìm thấy tệp");
        }
    }

    /** Hỗ trợ UC-3.8, UC-6.3 — Xóa tệp vật lý khỏi đĩa */
    public void delete(String relativePath) {
        try {
            Path filePath = Paths.get(attachmentProperties.getUploadDir()).resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Không thể xóa tệp");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tệp không được để trống");
        }
        if (file.getSize() > attachmentProperties.getMaxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tệp vượt quá dung lượng cho phép");
        }
    }

    public record StoredFile(String relativePath, String originalName, String contentType, long size) {
    }

}
