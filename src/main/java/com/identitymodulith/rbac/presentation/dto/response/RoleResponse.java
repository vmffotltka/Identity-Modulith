package com.identitymodulith.rbac.presentation.dto.response;

import com.identitymodulith.rbac.domain.RoleType;
import java.time.LocalDateTime;
import java.util.Set;

public record RoleResponse(
        String roleId,
        String name,
        RoleType type,
        String dataScopeLevel,
        String description,
        Boolean isActive,
        Set<PermissionResponse> permissions,
        Integer userCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 간소화 생성자 (목록 조회, 생성/복사 결과 반환용)
    public RoleResponse(String name, RoleType type, String description, Boolean isActive) {
        this(null, name, type, null, description, isActive, null, null, null, null);
    }
}

