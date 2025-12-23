package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Permission JPA Repository
 */
public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, String> {
}

