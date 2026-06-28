package com.workmanagement.backend.task.dto.request;

import com.workmanagement.backend.common.enums.PriorityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTaskRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    private String description;

    private PriorityLevel priority;

    private Long parentTaskId;

    private Long assigneeMemberId;

    private LocalDate deadline;

}
