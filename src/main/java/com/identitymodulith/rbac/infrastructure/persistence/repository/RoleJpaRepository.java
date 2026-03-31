package com.identitymodulith.rbac.infrastructure.persistence.repository;

import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 역할 엔티티 조회/저장 리포지토리. */
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    List<RoleJpaEntity> findByTenantId(String tenantId);

    Optional<RoleJpaEntity> findByTenantIdAndName(String tenantId, String name);

    boolean existsByTenantIdAndName(String tenantId, String name);

    Optional<RoleJpaEntity> findByTenantIdAndRoleId(String tenantId, String roleId);

    List<RoleJpaEntity> findByTenantIdAndNameIn(String tenantId, Collection<String> names);

    List<RoleJpaEntity> findByTenantIdAndIsActive(String tenantId, Boolean isActive);

    Optional<RoleJpaEntity> findByTenantIdAndNameAndIsActive(String tenantId, String name, Boolean isActive);
}
