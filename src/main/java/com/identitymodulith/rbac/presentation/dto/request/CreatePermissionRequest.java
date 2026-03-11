package com.identitymodulith.rbac.presentation.dto.request;

import com.identitymodulith.rbac.domain.RbacConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
        @NotBlank(message = "권한 코드는 필수입니다")
        @Pattern(regexp = "^[a-z]+:[a-z_]+$", message = "권한 코드는 'domain:action' 형식이어야 합니다 (예: user:create)")
        @Size(max = RbacConstants.PERMISSION_CODE_MAX_LENGTH, message = "권한 코드는 128자 이하여야 합니다")
        String code,

        @NotBlank(message = "권한명은 필수입니다")
        @Size(max = 100, message = "권한명은 100자 이하여야 합니다")
        String name,

        @Size(max = RbacConstants.PERMISSION_DESCRIPTION_MAX_LENGTH, message = "설명은 500자 이하여야 합니다")
        String description,

        @NotBlank(message = "카테고리는 필수입니다 (READ, WRITE, DELETE, ADMIN)")
        @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN)$", message = "카테고리는 READ, WRITE, DELETE, ADMIN 중 하나여야 합니다")
        String category,

        @Size(max = 100, message = "리소스는 100자 이하여야 합니다")
        String resource,

        @Size(max = 50, message = "액션은 50자 이하여야 합니다")
        String action
) {}

