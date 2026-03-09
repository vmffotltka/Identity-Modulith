package com.identitymodulith.rbac.application.service;

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
 * RBAC 관리 서비스 인터페이스
 */
public interface RbacManagementService {

    // ============================================================
    // 역할(Role) 관리
    // ============================================================

    List<RoleDto> getAllRoles();
    RoleDto getRoleByName(String roleName);
    RoleDto createRole(CreateRoleRequest request, String userId);
    RoleDto updateRole(String roleName, UpdateRoleRequest request, String userId);
    RoleDeletionResult deleteRole(String roleName, boolean forceDelete, String userId);
    RoleDeletionImpact getRoleDeletionImpact(String roleName);
    void deactivateRole(String roleName, String userId);
    void activateRole(String roleName, String userId);

    // ============================================================
    // 권한(Permission) 관리
    // ============================================================

    List<PermissionDto> getAllPermissions();
    PermissionDto getPermissionByCode(String code);
    PermissionDto createPermission(CreatePermissionRequest request, String userId);
    PermissionDto updatePermission(String code, UpdatePermissionRequest request, String userId);
    void deletePermission(String code, String userId);

    // ============================================================
    // 역할-권한 관계
    // ============================================================

    Set<PermissionDto> getPermissionsByRole(String roleName);
    void assignPermissionToRole(String roleName, String permissionCode, String userId);
    void revokePermissionFromRole(String roleName, String permissionCode, String userId);
    BatchAssignmentResult batchAssignPermissionsToRole(String roleName, Set<String> permissionCodes, String userId);
    BatchAssignmentResult batchRevokePermissionsFromRole(String roleName, Set<String> permissionCodes, String userId);

    // ============================================================
    // 사용자-역할 관계
    // ============================================================

    void assignRoleToAgent(String agentId, String roleName);
    void assignRoleToAgent(String agentId, String roleName, String userId);  // 권한 검증 포함
    void assignRoleToAgentByRoleId(String agentId, String roleId);  // 신규: roleId로 역할 할당
    void assignRoleToAgentWithoutAutoReplace(String agentId, String roleName);  // 신규: POSITION 자동 교체 없이 할당
    void revokeRoleFromAgent(String agentId, String roleName);
    void revokeRoleFromAgent(String agentId, String roleName, String userId);  // 권한 검증 포함
    void removeAllRolesFromAgent(String agentId);  // 신규: 사용자의 모든 역할 제거
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

    record RoleDto(
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

    record CreateRoleRequest(
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

    record UpdateRoleRequest(
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

    record PermissionDto(String code, String description, String category) {}

    record CreatePermissionRequest(
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

    record UpdatePermissionRequest(
            @Pattern(regexp = "^[a-z]+:[a-z_]+$", message = "권한 코드는 'domain:action' 형식이어야 합니다")
            @Size(max = RbacConstants.PERMISSION_CODE_MAX_LENGTH, message = "권한 코드는 128자 이하여야 합니다")
            String code,

            @Size(max = RbacConstants.PERMISSION_DESCRIPTION_MAX_LENGTH, message = "설명은 500자 이하여야 합니다")
            String description,

            @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN)$", message = "카테고리는 READ, WRITE, DELETE, ADMIN 중 하나여야 합니다")
            String category
    ) {}

    record BatchPermissionRequest(
            @NotNull(message = "권한 코드 목록은 필수입니다")
            Set<String> permissionCodes
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
