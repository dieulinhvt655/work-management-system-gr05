package com.workmanagement.backend.attachment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.attachment")
public class AttachmentProperties {

    private String uploadDir = "uploads";

    private long maxFileSizeBytes = 10 * 1024 * 1024;

}
