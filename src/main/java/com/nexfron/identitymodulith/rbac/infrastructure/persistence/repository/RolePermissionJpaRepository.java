package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {
    List<RolePermissionJpaEntity> findByRoleNameIn(Collection<String> roleNames);

    void deleteByRoleNameAndPermissionCode(String roleName, String permissionCode);

    void deleteByRoleName(String roleName);

    void deleteByPermissionCode(String permissionCode);
}


