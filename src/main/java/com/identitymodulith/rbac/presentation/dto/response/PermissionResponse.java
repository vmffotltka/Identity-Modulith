package com.identitymodulith.rbac.presentation.dto.response;

public record PermissionResponse(
        String code,
        String description,
        String category
) {}

