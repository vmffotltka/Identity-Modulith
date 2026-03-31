package com.identitymodulith.rbac.application.service;

import com.identitymodulith.rbac.presentation.dto.request.*;
import com.identitymodulith.rbac.presentation.dto.response.*;

import java.util.List;
import java.util.Set;

/**
 * RBAC 관리 서비스 인터페이스
 */
public interface RbacManagementService {

    List<RoleResponse> getAllRoles();
    RoleResponse getRoleByName(String roleName);
    RoleResponse createRole(CreateRoleRequest request, String userId);
    RoleResponse updateRole(String roleName, UpdateRoleRequest request, String userId);
    RoleDeletionResponse deleteRole(String roleName, boolean forceDelete, String userId);
    RoleDeletionImpactResponse getRoleDeletionImpact(String roleName);
    void deactivateRole(String roleName, String userId);
    void activateRole(String roleName, String userId);

    List<PermissionResponse> getAllPermissions();
    PermissionResponse getPermissionByCode(String code);
    PermissionResponse createPermission(CreatePermissionRequest request, String userId);
    PermissionResponse updatePermission(String code, UpdatePermissionRequest request, String userId);
    void deletePermission(String code, String userId);

    Set<PermissionResponse> getPermissionsByRole(String roleName);
    void assignPermissionToRole(String roleName, String permissionCode, String userId);
    void revokePermissionFromRole(String roleName, String permissionCode, String userId);
    BatchAssignmentResponse batchAssignPermissionsToRole(String roleName, Set<String> permissionCodes, String userId);
    BatchAssignmentResponse batchRevokePermissionsFromRole(String roleName, Set<String> permissionCodes, String userId);

    void assignRoleToAgent(String agentId, String roleName);
    void assignRoleToAgent(String agentId, String roleName, String userId);
    void assignRoleToAgentByRoleId(String agentId, String roleId);
    void assignRoleToAgentWithoutAutoReplace(String agentId, String roleName);
    void revokeRoleFromAgent(String agentId, String roleName);
    void revokeRoleFromAgent(String agentId, String roleName, String userId);
    void removeAllRolesFromAgent(String agentId);
    Set<String> getRolesByAgent(String agentId);
    boolean hasRole(String agentId, String roleName);
    Set<String> getEffectivePermissions(String agentId);
    int getAgentCountByRole(String roleName);

    Set<String> getRolesWithPermission(String permissionCode);


    RoleResponse cloneRole(String sourceRoleName, CloneRoleRequest request);
}
