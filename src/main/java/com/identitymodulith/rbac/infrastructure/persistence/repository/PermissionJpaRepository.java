package com.identitymodulith.rbac.infrastructure.persistence.repository;

import com.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 권한 엔티티 조회/저장 리포지토리. */
public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, String> {

    List<PermissionJpaEntity> findByTenantId(String tenantId);

    Optional<PermissionJpaEntity> findByTenantIdAndCode(String tenantId, String code);

    boolean existsByTenantIdAndCode(String tenantId, String code);

    /** 권한 ID 목록을 테넌트 조건으로 배치 조회한다. */
    List<PermissionJpaEntity> findByTenantIdAndPermissionIdIn(String tenantId, Collection<String> permissionIds);
}
