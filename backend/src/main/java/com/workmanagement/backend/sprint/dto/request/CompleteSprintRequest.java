package com.workmanagement.backend.sprint.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteSprintRequest {

    @NotBlank(message = "Tổng kết sprint không được để trống")
    private String summary;

}
