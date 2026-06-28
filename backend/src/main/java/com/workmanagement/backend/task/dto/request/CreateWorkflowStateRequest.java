package com.workmanagement.backend.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkflowStateRequest {

    @NotBlank(message = "Tên trạng thái không được để trống")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Mã trạng thái không được để trống")
    @Size(max = 100)
    private String code;

    @NotNull(message = "Vị trí không được để trống")
    private Integer position;

    private Boolean isDefault;

    private Boolean isFinal;

}
