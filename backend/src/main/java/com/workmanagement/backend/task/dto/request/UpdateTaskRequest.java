package com.workmanagement.backend.task.dto.request;

import com.workmanagement.backend.common.enums.PriorityLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateTaskRequest {

    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    private String description;

    private PriorityLevel priority;

    private LocalDate startDate;

    private LocalDate deadline;

    @Min(value = 0, message = "Tiến độ tối thiểu 0")
    @Max(value = 100, message = "Tiến độ tối đa 100")
    private Integer progress;

}
