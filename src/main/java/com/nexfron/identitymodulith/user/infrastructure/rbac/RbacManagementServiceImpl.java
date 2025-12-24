package com.nexfron.identitymodulith.user.infrastructure.rbac;

import com.nexfron.identitymodulith.rbac.application.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.PermissionDto;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.RoleDto;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.CreatePermissionRequest;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.CreateRoleRequest;
import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RbacManagementServiceImpl implements RbacManagementService {

    private final RolePermissionJpaRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleDto(role.getName(), role.getType()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleByName(String roleName) {
        return roleRepository.findById(roleName)
                .map(role -> new RoleDto(role.getName(), role.getType()))
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));
    }

    @Override
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsById(request.name())) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_ALREADY_EXISTS);
        }
        RoleJpaEntity role = RoleJpaEntity.builder().name(request.name()).type(request.type()).build();
        RoleJpaEntity saved = roleRepository.save(role);
        return new RoleDto(saved.getName(), saved.getType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(perm -> new PermissionDto(perm.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionDto getPermissionByCode(String code) {
        return permissionRepository.findById(code)
                .map(perm -> new PermissionDto(perm.getCode()))
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));
    }

    @Override
    public PermissionDto createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsById(request.code())) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_EXISTS);
        }
        PermissionJpaEntity permission = PermissionJpaEntity.builder().code(request.code()).build();
        PermissionJpaEntity saved = permissionRepository.save(permission);
        return new PermissionDto(saved.getCode());
    }

    @Override
    public void assignPermissionToRole(String roleName, String permissionCode) {
        if (!roleRepository.existsById(roleName)) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
        }
        if (!permissionRepository.existsById(permissionCode)) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND);
        }
        boolean alreadyExists = rolePermissionRepository.findByRoleNameIn(Set.of(roleName)).stream()
                .anyMatch(rp -> rp.getPermissionCode().equals(permissionCode));
        if (alreadyExists) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }
        RolePermissionJpaEntity rolePermission = RolePermissionJpaEntity.builder()
                .roleName(roleName).permissionCode(permissionCode).build();
        rolePermissionRepository.save(rolePermission);
    }

    @Override
    public void revokePermissionFromRole(String roleName, String permissionCode) {
        rolePermissionRepository.deleteByRoleNameAndPermissionCode(roleName, permissionCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PermissionDto> getPermissionsByRole(String roleName) {
        return rolePermissionRepository.findByRoleNameIn(Set.of(roleName)).stream()
                .map(rp -> new PermissionDto(rp.getPermissionCode()))
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteRole(String roleName) {
        if (!roleRepository.existsById(roleName)) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
        }
        rolePermissionRepository.deleteByRoleName(roleName);
        roleRepository.deleteById(roleName);
    }

    @Override
    public void deletePermission(String code) {
        if (!permissionRepository.existsById(code)) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND);
        }
        rolePermissionRepository.deleteByPermissionCode(code);
        permissionRepository.deleteById(code);
    }

    interface RoleRepository extends JpaRepository<RoleJpaEntity, String> {}
    interface PermissionRepository extends JpaRepository<PermissionJpaEntity, String> {}
    interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {
        List<RolePermissionJpaEntity> findByRoleNameIn(java.util.Collection<String> roleNames);
        void deleteByRoleNameAndPermissionCode(String roleName, String permissionCode);
        void deleteByRoleName(String roleName);
        void deleteByPermissionCode(String permissionCode);
    }
}

