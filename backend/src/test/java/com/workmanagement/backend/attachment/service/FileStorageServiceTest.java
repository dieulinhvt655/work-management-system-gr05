package com.workmanagement.backend.attachment.service;

import com.workmanagement.backend.attachment.config.AttachmentProperties;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private AttachmentProperties attachmentProperties;

    @InjectMocks
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @Test
    void storeProjectFile_shouldSaveFile() {
        when(attachmentProperties.getUploadDir()).thenReturn(tempDir.toString());
        when(attachmentProperties.getMaxFileSizeBytes()).thenReturn(1024L);
        MultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes());

        FileStorageService.StoredFile stored = fileStorageService.storeProjectFile(30L, file);

        assertThat(stored.originalName()).isEqualTo("doc.txt");
        assertThat(Files.exists(tempDir.resolve(stored.relativePath()))).isTrue();
    }

    @Test
    void storeProjectFile_shouldRejectOversizedFile() {
        when(attachmentProperties.getMaxFileSizeBytes()).thenReturn(1024L);
        MultipartFile file = new MockMultipartFile("file", "big.bin", "application/octet-stream", new byte[2048]);

        assertThatThrownBy(() -> fileStorageService.storeProjectFile(30L, file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

}
