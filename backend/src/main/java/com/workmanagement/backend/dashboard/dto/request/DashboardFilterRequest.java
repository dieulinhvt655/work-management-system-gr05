package com.workmanagement.backend.dashboard.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardFilterRequest {

    private Integer upcomingLimit = 5;

}
