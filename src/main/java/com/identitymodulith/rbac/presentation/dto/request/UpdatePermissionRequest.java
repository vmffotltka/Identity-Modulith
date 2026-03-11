package com.identitymodulith.rbac.presentation.dto.request;

import com.identitymodulith.rbac.domain.RbacConstants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePermissionRequest(
        @Pattern(regexp = "^[a-z]+:[a-z_]+$", message = "권한 코드는 'domain:action' 형식이어야 합니다")
        @Size(max = RbacConstants.PERMISSION_CODE_MAX_LENGTH, message = "권한 코드는 128자 이하여야 합니다")
        String code,

        @Size(max = RbacConstants.PERMISSION_DESCRIPTION_MAX_LENGTH, message = "설명은 500자 이하여야 합니다")
        String description,

        @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN)$", message = "카테고리는 READ, WRITE, DELETE, ADMIN 중 하나여야 합니다")
        String category
) {}

