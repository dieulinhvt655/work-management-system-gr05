package com.workmanagement.backend.project.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProjectRequest {

    @Size(max = 255, message = "Tên dự án tối đa 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 2000, message = "Mục tiêu tối đa 2000 ký tự")
    private String objective;

    @Size(max = 2000, message = "Phạm vi tối đa 2000 ký tự")
    private String scope;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long projectManagerMemberId;

}
