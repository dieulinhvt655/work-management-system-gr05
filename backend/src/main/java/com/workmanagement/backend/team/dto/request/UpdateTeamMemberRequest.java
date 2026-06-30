package com.workmanagement.backend.team.dto.request;

import com.workmanagement.backend.common.enums.MemberStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeamMemberRequest {

    /** Tùy chọn: chỉ truyền khi muốn đổi vai trò. Cho phép cập nhật riêng {@code status}. */
    private Long roleId;

    private MemberStatus status;

}
