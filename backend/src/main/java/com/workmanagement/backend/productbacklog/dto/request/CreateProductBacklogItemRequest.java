package com.workmanagement.backend.productbacklog.dto.request;

import com.workmanagement.backend.common.enums.PbiType;
import com.workmanagement.backend.common.enums.PriorityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateProductBacklogItemRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    private String description;

    private PbiType type;

    private PriorityLevel priority;

    private LocalDate desiredDueDate;

    private Long proposerMemberId;

}
