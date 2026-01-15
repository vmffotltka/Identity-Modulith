package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.application.dto.AuditLogDto;
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

    // ============================================================
    // 사용자-역할 관계
    // ============================================================

    void assignRoleToAgent(String agentId, String roleName);
    void revokeRoleFromAgent(String agentId, String roleName);
    Set<String> getRolesByAgent(String agentId);
    int getAgentCountByRole(String roleName);

    // ============================================================
    // 권한 그룹 관리
    // ============================================================

    List<PermissionGroupDto> getAllPermissionGroups();
    PermissionGroupDto getPermissionGroupByName(String groupName);
    PermissionGroupDto createPermissionGroup(CreatePermissionGroupRequest request);
    PermissionGroupDto updatePermissionGroup(String groupName, UpdatePermissionGroupRequest request);
    void deactivatePermissionGroup(String groupName);
    void activatePermissionGroup(String groupName);

    void addPermissionToGroup(String groupName, String permissionCode);
    void removePermissionFromGroup(String groupName, String permissionCode);
    void assignPermissionGroupToRole(String roleName, String groupName);
    void revokePermissionGroupFromRole(String roleName, String groupName);

    // ============================================================
    // 권한 변경 이력 조회 (Audit Log)
    // ============================================================

    List<AuditLogDto> getAgentPermissionChangeHistory(String agentId, LocalDateTime from, LocalDateTime to);
    List<AuditLogDto> getRolePermissionChangeHistory(String roleName, LocalDateTime from, LocalDateTime to);
    List<AuditLogDto> getAllPermissionChangeHistory(LocalDateTime from, LocalDateTime to, Integer pageSize);
    List<AuditLogDto> getOperatorPermissionActions(String operatorId, LocalDateTime from, LocalDateTime to);

    // ============================================================
    // DTOs
    // ============================================================

    record RoleDto(String name, String type, String description, Boolean isActive) {}

    record CreateRoleRequest(String name, String type) {}

    record UpdateRoleRequest(String type, String description, Boolean isActive) {
        public UpdateRoleRequest(String type) {
            this(type, null, null);
        }
    }

    record PermissionDto(String code, String description) {}

    record CreatePermissionRequest(String code, String description) {}

    record UpdatePermissionRequest(String code, String description) {}

    record PermissionGroupDto(String name, String description, Boolean isActive) {
        public PermissionGroupDto(String name) {
            this(name, null, true);
        }
    }

    record CreatePermissionGroupRequest(String name, String description) {
        public CreatePermissionGroupRequest(String name) {
            this(name, null);
        }
    }

    record UpdatePermissionGroupRequest(String description, Boolean isActive) {}

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
}
