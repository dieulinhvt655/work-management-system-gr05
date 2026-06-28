package com.workmanagement.backend.productbacklog.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductBacklogRequest {

    @Size(max = 255, message = "Tên backlog tối đa 255 ký tự")
    private String name;

    private String description;

}
