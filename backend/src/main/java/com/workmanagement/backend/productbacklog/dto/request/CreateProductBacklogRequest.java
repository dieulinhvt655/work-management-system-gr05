package com.workmanagement.backend.productbacklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductBacklogRequest {

    @NotBlank(message = "Tên backlog không được để trống")
    @Size(max = 255, message = "Tên backlog tối đa 255 ký tự")
    private String name;

    private String description;

}
