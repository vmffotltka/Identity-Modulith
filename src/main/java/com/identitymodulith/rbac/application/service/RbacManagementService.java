package com.identitymodulith.rbac.application.service;

import com.identitymodulith.rbac.presentation.dto.RbacDto.*;

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


}
