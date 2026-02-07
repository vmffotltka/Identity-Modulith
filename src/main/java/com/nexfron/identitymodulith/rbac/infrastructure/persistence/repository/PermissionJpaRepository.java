package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 권한(Permission) JPA Repository
 *
 * 권한 엔티티의 데이터 접근을 담당합니다.
 * Spring Data JPA의 기본 CRUD 메서드들을 상속받아 사용하며,
 * 필요시 커스텀 쿼리 메서드를 추가할 수 있습니다.
 *
 * 기본 메서드:
 * - findById(String code): 권한 코드로 권한 조회
 * - save(PermissionJpaEntity): 권한 저장 또는 수정
 * - delete(PermissionJpaEntity): 권한 삭제
 * - findAll(): 모든 권한 조회
 * - existsById(String code): 권한 존재 여부 확인
 *
 * 주의:
 * 권한을 삭제할 때는 해당 권한이 할당된 역할이 없는지
 * 먼저 확인해야 합니다. (role_permissions 테이블 참고)
 *
 * @see PermissionJpaEntity
 * @see RolePermissionJpaRepository
 */
public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, String> {

    /**
     * 특정 테넌트의 모든 권한 조회
     *
     * @param tenantId 테넌트 ID
     * @return 권한 리스트 (빈 리스트 가능)
     */
    List<PermissionJpaEntity> findByTenantId(String tenantId);

    /**
     * 테넌트와 권한 코드로 특정 권한 조회
     *
     * @param tenantId 테넌트 ID
     * @param code 권한 코드 (예: "user:create", "org:view")
     * @return Optional 권한
     */
    Optional<PermissionJpaEntity> findByTenantIdAndCode(String tenantId, String code);

    /**
     * 테넌트와 권한 코드 중복 확인
     *
     * @param tenantId 테넌트 ID
     * @param code 권한 코드
     * @return 존재 여부
     */
    boolean existsByTenantIdAndCode(String tenantId, String code);

    /**
     * [추가] 테넌트와 권한 ID 목록으로 권한 엔티티를 배치 조회합니다.
     * @param tenantId 테넌트 ID
     * @param permissionIds 권한 ID 목록
     * @return 권한 엔티티 리스트(코드 포함)
     */
    List<PermissionJpaEntity> findByTenantIdAndPermissionIdIn(String tenantId, Collection<String> permissionIds);
}
