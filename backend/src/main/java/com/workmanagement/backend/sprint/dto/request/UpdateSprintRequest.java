package com.workmanagement.backend.sprint.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateSprintRequest {

    @Size(max = 255, message = "Tên sprint tối đa 255 ký tự")
    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

}
