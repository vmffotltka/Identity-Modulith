package com.identitymodulith.rbac.presentation.dto.request;

import com.identitymodulith.rbac.domain.RbacConstants;
import com.identitymodulith.rbac.domain.RoleType;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        RoleType type,

        @Size(max = RbacConstants.ROLE_DESCRIPTION_MAX_LENGTH, message = "설명은 255자 이하여야 합니다")
        String description,

        String dataScopeLevel,

        Boolean isActive
) {
    public UpdateRoleRequest(RoleType type) {
        this(type, null, null, null);
    }
}

