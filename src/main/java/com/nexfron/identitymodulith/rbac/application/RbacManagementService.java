package com.nexfron.identitymodulith.rbac.application;

import java.util.List;
import java.util.Set;

/**
 * RBAC 관리 서비스 인터페이스
 */
public interface RbacManagementService {

    List<RoleDto> getAllRoles();

    RoleDto getRoleByName(String roleName);

    RoleDto createRole(CreateRoleRequest request);

    List<PermissionDto> getAllPermissions();

    PermissionDto getPermissionByCode(String code);

    PermissionDto createPermission(CreatePermissionRequest request);

    void assignPermissionToRole(String roleName, String permissionCode);

    void revokePermissionFromRole(String roleName, String permissionCode);

    Set<PermissionDto> getPermissionsByRole(String roleName);

    void deleteRole(String roleName);

    void deletePermission(String code);

    record RoleDto(String name, String type) {}
    record PermissionDto(String code) {}
    record CreateRoleRequest(String name, String type) {}

    record CreatePermissionRequest(String code) {}
}

