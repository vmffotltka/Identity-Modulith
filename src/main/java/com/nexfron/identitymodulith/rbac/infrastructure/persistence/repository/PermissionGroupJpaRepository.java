package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 권한 그룹(Permission Group) JPA Repository
 *
 * 권한 그룹 엔티티의 영속화를 담당합니다.
 * 멀티테넌시 환경에서 tenantId 기반 격리를 지원합니다.
 */
public interface PermissionGroupJpaRepository extends JpaRepository<PermissionGroupJpaEntity, String> {

    /**
     * 특정 테넌트의 모든 활성 권한 그룹 조회
     *
     * @param tenantId 테넌트 ID
     * @param isActive 활성화 상태
     * @return 권한 그룹 리스트
     */
    List<PermissionGroupJpaEntity> findByTenantIdAndIsActive(String tenantId, Boolean isActive);

    /**
     * 테넌트와 그룹명으로 권한 그룹 조회
     *
     * @param tenantId 테넌트 ID
     * @param name 그룹명
     * @return Optional 권한 그룹
     */
    Optional<PermissionGroupJpaEntity> findByTenantIdAndName(String tenantId, String name);

    /**
     * 테넌트와 그룹명 중복 확인
     *
     * @param tenantId 테넌트 ID
     * @param name 그룹명
     * @return 존재 여부
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * 테넌트와 권한 그룹 ID로 조회
     *
     * @param tenantId 테넌트 ID
     * @param permissionGroupId 권한 그룹 ID
     * @return Optional 권한 그룹
     */
    Optional<PermissionGroupJpaEntity> findByTenantIdAndPermissionGroupId(String tenantId, String permissionGroupId);

    /**
     * 특정 테넌트의 모든 권한 그룹 조회
     *
     * @param tenantId 테넌트 ID
     * @return 권한 그룹 리스트
     */
    List<PermissionGroupJpaEntity> findByTenantId(String tenantId);

    /**
     * 특정 테넌트의 권한 그룹들 조회 (여러 개)
     *
     * @param tenantId 테넌트 ID
     * @param names 그룹명 집합
     * @return 권한 그룹 리스트
     */
    List<PermissionGroupJpaEntity> findByTenantIdAndNameIn(String tenantId, Collection<String> names);
}

