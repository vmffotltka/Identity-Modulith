package com.identitymodulith.rbac.presentation.dto.response;

public record RoleDeletionResponse(
        String roleName,
        int affectedUserCount,
        int removedPermissionCount,
        boolean forceDeleted,
        String warningMessage
) {}

