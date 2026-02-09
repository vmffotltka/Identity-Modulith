package com.nexfron.identitymodulith.rbac.application.service;

import com.nexfron.identitymodulith.rbac.domain.RbacConstants;
import com.nexfron.identitymodulith.rbac.domain.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * RBAC 관리 서비스 인터페이스
 */
public interface RbacManagementService {

    // ============================================================
    // 역할(Role) 관리
    // ============================================================

    List<RoleDto> getAllRoles();
    RoleDto getRoleByName(String roleName);
    RoleDto createRole(CreateRoleRequest request);
    RoleDto updateRole(String roleName, UpdateRoleRequest request);
    RoleDeletionResult deleteRole(String roleName, boolean forceDelete);
    RoleDeletionImpact getRoleDeletionImpact(String roleName);
    void deactivateRole(String roleName);
    void activateRole(String roleName);

    // ============================================================
    // 권한(Permission) 관리
    // ============================================================

    List<PermissionDto> getAllPermissions();
    PermissionDto getPermissionByCode(String code);
    PermissionDto createPermission(CreatePermissionRequest request);
    PermissionDto updatePermission(String code, UpdatePermissionRequest request);
    void deletePermission(String code);

    // ============================================================
    // 역할-권한 관계
    // ============================================================

    Set<PermissionDto> getPermissionsByRole(String roleName);
    void assignPermissionToRole(String roleName, String permissionCode);
    void revokePermissionFromRole(String roleName, String permissionCode);
    BatchAssignmentResult batchAssignPermissionsToRole(String roleName, Set<String> permissionCodes);
    BatchAssignmentResult batchRevokePermissionsFromRole(String roleName, Set<String> permissionCodes);

    // ============================================================
    // 사용자-역할 관계
    // ============================================================

    void assignRoleToAgent(String agentId, String roleName);
    void revokeRoleFromAgent(String agentId, String roleName);
    Set<String> getRolesByAgent(String agentId);
    boolean hasRole(String agentId, String roleName);  // 신규: 특정 역할 보유 확인
    Set<String> getEffectivePermissions(String agentId);  // 신규: 사용자의 실제 권한 조회
    int getAgentCountByRole(String roleName);

    // ============================================================
    // 권한-역할 역검색
    // ============================================================

    Set<String> getRolesWithPermission(String permissionCode);  // 신규: 권한을 가진 역할 조회

    // ============================================================
    // 역할 복사
    // ============================================================

    RoleDto cloneRole(String sourceRoleName, CloneRoleRequest request);  // 신규: 역할 복사



    // ============================================================
    // DTOs
    // ============================================================

    record RoleDto(String name, RoleType type, String description, Boolean isActive) {}

    record CreateRoleRequest(
            @NotBlank(message = "역할명은 필수입니다")
            @Size(
                min = RbacConstants.ROLE_NAME_MIN_LENGTH,
                max = RbacConstants.ROLE_NAME_MAX_LENGTH,
                message = "역할명은 2-64자 사이여야 합니다"
            )
            String name,

            RoleType type
    ) {}

    record UpdateRoleRequest(
            RoleType type,

            @Size(max = RbacConstants.ROLE_DESCRIPTION_MAX_LENGTH, message = "설명은 255자 이하여야 합니다")
            String description,

            Boolean isActive
    ) {
        public UpdateRoleRequest(RoleType type) {
            this(type, null, null);
        }
    }

    record PermissionDto(String code, String description, String category) {}

    record CreatePermissionRequest(
            @NotBlank(message = "권한 코드는 필수입니다")
            @Pattern(regexp = "^[a-z]+:[a-z_]+$", message = "권한 코드는 'domain:action' 형식이어야 합니다 (예: user:create)")
            @Size(max = RbacConstants.PERMISSION_CODE_MAX_LENGTH, message = "권한 코드는 128자 이하여야 합니다")
            String code,

            @Size(max = RbacConstants.PERMISSION_DESCRIPTION_MAX_LENGTH, message = "설명은 500자 이하여야 합니다")
            String description,

            @NotBlank(message = "카테고리는 필수입니다 (READ, WRITE, DELETE, ADMIN)")
            @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN)$", message = "카테고리는 READ, WRITE, DELETE, ADMIN 중 하나여야 합니다")
            String category
    ) {}

    record UpdatePermissionRequest(
            @Pattern(regexp = "^[a-z]+:[a-z_]+$", message = "권한 코드는 'domain:action' 형식이어야 합니다")
            @Size(max = RbacConstants.PERMISSION_CODE_MAX_LENGTH, message = "권한 코드는 128자 이하여야 합니다")
            String code,

            @Size(max = RbacConstants.PERMISSION_DESCRIPTION_MAX_LENGTH, message = "설명은 500자 이하여야 합니다")
            String description,

            @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN)$", message = "카테고리는 READ, WRITE, DELETE, ADMIN 중 하나여야 합니다")
            String category
    ) {}


    record CloneRoleRequest(
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


    record RoleDeletionResult(
            String roleName,
            int affectedUserCount,
            int removedPermissionCount,
            boolean forceDeleted,
            String warningMessage
    ) {}

    record RoleDeletionImpact(
            String roleName,
            int affectedUserCount,
            int assignedPermissionCount,
            boolean canDelete,
            String impactDetails
    ) {}

    record BatchAssignmentResult(
            int successCount,
            int failedCount,
            int skippedCount,
            List<String> errors
    ) {}
}
