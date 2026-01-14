package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Role JPA Repository
 *
 * 역할(Role) 엔티티의 영속화를 담당합니다.
 * 멀티테넌시 환경에서 tenantId 기반 격리를 지원합니다.
 */
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    /**
     * 특정 테넌트의 모든 역할 조회
     *
     * @param tenantId 테넌트 ID
     * @return 역할 리스트 (빈 리스트 가능)
     */
    List<RoleJpaEntity> findByTenantId(String tenantId);

    /**
     * 테넌트와 역할명으로 특정 역할 조회
     *
     * @param tenantId 테넌트 ID
     * @param name 역할명
     * @return Optional 역할
     */
    Optional<RoleJpaEntity> findByTenantIdAndName(String tenantId, String name);

    /**
     * 테넌트와 역할명 중복 확인
     *
     * @param tenantId 테넌트 ID
     * @param name 역할명
     * @return 존재 여부
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * [추가] 테넌트와 역할 ID로 역할 단건 조회 (tenant-aware)
     *
     * @param tenantId 테넌트 ID
     * @param roleId   역할 ID(UUID 문자열)
     */
    Optional<RoleJpaEntity> findByTenantIdAndRoleId(String tenantId, String roleId);

    /**
     * [추가] 테넌트와 여러 역할명으로 역할 목록 조회 (permissionsOfRoles 등에서 사용)
     *
     * @param tenantId  테넌트 ID
     * @param names     역할명 집합
     * @return 해당 테넌트에 속한 역할들만 반환
     */
    List<RoleJpaEntity> findByTenantIdAndNameIn(String tenantId, Collection<String> names);
}
