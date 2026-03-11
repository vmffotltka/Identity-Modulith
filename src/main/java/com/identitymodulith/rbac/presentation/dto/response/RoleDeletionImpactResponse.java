package com.identitymodulith.rbac.presentation.dto.response;

public record RoleDeletionImpactResponse(
        String roleName,
        int affectedUserCount,
        int assignedPermissionCount,
        boolean canDelete,
        String impactDetails
) {}

