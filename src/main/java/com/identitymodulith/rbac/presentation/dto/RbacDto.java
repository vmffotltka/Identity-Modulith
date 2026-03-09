package com.identitymodulith.rbac.presentation.dto;

import com.identitymodulith.rbac.domain.RbacConstants;
import com.identitymodulith.rbac.domain.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * RBAC API에서 사용하는 DTO 모음
 */
public class RbacDto {

    // ============================================================
    // 역할(Role) DTO
    // ============================================================

    public record RoleDto(
            String roleId,
            String name,
            RoleType type,
            String dataScopeLevel,
            String description,
            Boolean isActive,
            Set<PermissionDto> permissions,
            Integer userCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        // 간단한 버전 (목록 조회용)
        public RoleDto(String name, RoleType type, String description, Boolean isActive) {
            this(null, name, type, null, description, isActive, null, null, null, null);
        }
    }

    public record CreateRoleRequest(
            @NotBlank(message = "역할명은 필수입니다")
            @Size(
                min = RbacConstants.ROLE_NAME_MIN_LENGTH,
                max = RbacConstants.ROLE_NAME_MAX_LENGTH,
                message = "역할명은 2-64자 사이여야 합니다"
            )
            String name,

            RoleType type,

            @Size(max = RbacConstants.ROLE_DESCRIPTION_MAX_LENGTH, message = "설명은 255자 이하여야 합니다")
            String description
    ) {}

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

    public record RoleDeletionResult(
            String roleName,
            int affectedUserCount,
            int removedPermissionCount,
            boolean forceDeleted,
            String warningMessage
    ) {}

    public record RoleDeletionImpact(
            String roleName,
            int affectedUserCount,
            int assignedPermissionCount,
            boolean canDelete,
            String impactDetails
    ) {}

    public record CloneRoleRequest(
            @NotBlank(message = "새 역할명은 필수입니다")
            @Size(
                min = RbacConstants.ROLE_NAME_MIN_LENGTH,
                max = RbacConstants.ROLE_NAME_MAX_LENGTH,
                message = "역할명은 2-64자 사이여야 합니다"
            )
            String newRoleName,

            @Size(max = RbacConstants.ROLE_DESCRIPTION_MAX_LENGTH, message = "설명은 255자 이하여야 합니다")
            String description
    ) {}

    // ============================================================
    // 권한(Permission) DTO
    // ============================================================

    public record PermissionDto(String code, String description, String category) {}

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

    public record UpdatePermissionRequest(
            @Pattern(regexp = "^[a-z]+:[a-z_]+$", message = "권한 코드는 'domain:action' 형식이어야 합니다")
            @Size(max = RbacConstants.PERMISSION_CODE_MAX_LENGTH, message = "권한 코드는 128자 이하여야 합니다")
            String code,

            @Size(max = RbacConstants.PERMISSION_DESCRIPTION_MAX_LENGTH, message = "설명은 500자 이하여야 합니다")
            String description,

            @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN)$", message = "카테고리는 READ, WRITE, DELETE, ADMIN 중 하나여야 합니다")
            String category
    ) {}

    // ============================================================
    // 공통 DTO
    // ============================================================

    public record BatchPermissionRequest(
            @NotNull(message = "권한 코드 목록은 필수입니다")
            Set<String> permissionCodes
    ) {}

    public record BatchAssignmentResult(
            int successCount,
            int failedCount,
            int skippedCount,
            List<String> errors
    ) {}
}

