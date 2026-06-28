package com.workmanagement.backend.task.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskProgressRequest {

    @NotNull(message = "Tiến độ không được để trống")
    @Min(value = 0, message = "Tiến độ tối thiểu 0")
    @Max(value = 100, message = "Tiến độ tối đa 100")
    private Integer progress;

}
