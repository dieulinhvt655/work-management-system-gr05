package com.workmanagement.backend.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkspaceRequest {

    @NotBlank(message = "Tên workspace không được để trống")
    @Size(max = 255, message = "Tên workspace tối đa 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "Workspace owner không được để trống")
    @Positive(message = "Workspace owner không hợp lệ")
    private Long ownerId;

}
