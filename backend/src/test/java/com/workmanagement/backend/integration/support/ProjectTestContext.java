package com.workmanagement.backend.integration.support;

public record ProjectTestContext(
        LoginTokens tokens,
        Long workspaceId,
        Long teamId,
        Long projectId,
        Long teamMemberId
) {
}
