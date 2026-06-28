package com.workmanagement.backend.attachment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAttachmentRequest {

    @NotBlank(message = "Tên tệp không được để trống")
    @Size(max = 255, message = "Tên tệp tối đa 255 ký tự")
    private String fileName;

}
