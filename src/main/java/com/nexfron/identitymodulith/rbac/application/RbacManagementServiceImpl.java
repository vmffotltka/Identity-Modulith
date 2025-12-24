package com.nexfron.identitymodulith.rbac.application;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class RbacManagementServiceImpl implements RbacManagementService {

    @Override
    public List<RoleDto> getAllRoles() { return List.of(); }

    @Override
    public RoleDto getRoleByName(String roleName) { return new RoleDto(roleName, "SYSTEM"); }

    @Override
    public RoleDto createRole(CreateRoleRequest request) { return new RoleDto(request.name(), request.type()); }

    @Override
    public List<PermissionDto> getAllPermissions() { return List.of(); }

    @Override
    public PermissionDto getPermissionByCode(String code) { return new PermissionDto(code); }

    @Override
    public PermissionDto createPermission(CreatePermissionRequest request) { return new PermissionDto(request.code()); }

    @Override
    public void assignPermissionToRole(String roleName, String permissionCode) {}

    @Override
    public void revokePermissionFromRole(String roleName, String permissionCode) {}

    @Override
    public Set<PermissionDto> getPermissionsByRole(String roleName) { return Set.of(); }

    @Override
    public void deleteRole(String roleName) {}

    @Override
    public void deletePermission(String code) {}
}
