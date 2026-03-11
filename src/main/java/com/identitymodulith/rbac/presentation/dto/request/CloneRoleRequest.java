package com.identitymodulith.rbac.presentation.dto.request;

import com.identitymodulith.rbac.domain.RbacConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloneRoleRequest(
        @NotBlank(message = "새 역할명은 필수입니다")
        @Size(min = RbacConstants.ROLE_NAME_MIN_LENGTH, max = RbacConstants.ROLE_NAME_MAX_LENGTH,
              message = "역할명은 2-64자 사이여야 합니다")
        String newRoleName,

        @Size(max = RbacConstants.ROLE_DESCRIPTION_MAX_LENGTH, message = "설명은 255자 이하여야 합니다")
        String description
) {}

