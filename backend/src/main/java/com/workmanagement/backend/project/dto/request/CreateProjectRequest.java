package com.workmanagement.backend.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    @Size(max = 255, message = "Tên dự án tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Mục tiêu không được để trống")
    @Size(max = 2000, message = "Mục tiêu tối đa 2000 ký tự")
    private String objective;

    @NotBlank(message = "Phạm vi không được để trống")
    @Size(max = 2000, message = "Phạm vi tối đa 2000 ký tự")
    private String scope;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "startDate không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    @Positive(message = "projectManagerMemberId không hợp lệ")
    private Long projectManagerMemberId;

}
