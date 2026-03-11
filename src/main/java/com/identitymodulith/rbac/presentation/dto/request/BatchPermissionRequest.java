package com.identitymodulith.rbac.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record BatchPermissionRequest(
        @NotNull(message = "권한 코드 목록은 필수입니다")
        Set<String> permissionCodes
) {}

