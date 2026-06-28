package com.workmanagement.backend.sprint.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateSprintRequest {

    @NotBlank(message = "Tên sprint không được để trống")
    @Size(max = 255, message = "Tên sprint tối đa 255 ký tự")
    private String name;

    private String goal;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

}
