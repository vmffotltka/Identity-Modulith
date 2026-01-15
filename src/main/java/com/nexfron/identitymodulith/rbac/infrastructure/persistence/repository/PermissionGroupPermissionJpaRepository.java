package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionGroupPermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 권한 그룹-권한 매핑 JPA Repository
 *
 * 권한 그룹과 권한의 다대다 관계를 관리합니다.
 */
public interface PermissionGroupPermissionJpaRepository extends JpaRepository<PermissionGroupPermissionJpaEntity, Long> {

    /**
     * 특정 권한 그룹의 모든 권한 ID 조회
     *
     * @param permissionGroupId 권한 그룹 ID
     * @return 권한 ID 리스트
     */
    List<PermissionGroupPermissionJpaEntity> findByPermissionGroupId(String permissionGroupId);

    /**
     * 특정 권한이 속한 모든 그룹 조회
     *
     * @param permissionId 권한 ID
     * @return 매핑 리스트
     */
    List<PermissionGroupPermissionJpaEntity> findByPermissionId(String permissionId);

    /**
     * 그룹-권한 조합 존재 확인
     *
     * @param permissionGroupId 권한 그룹 ID
     * @param permissionId 권한 ID
     * @return 존재 여부
     */
    boolean existsByPermissionGroupIdAndPermissionId(String permissionGroupId, String permissionId);

    /**
     * 권한 그룹의 모든 매핑 삭제 (그룹 삭제 시 사용)
     *
     * @param permissionGroupId 권한 그룹 ID
     */
    void deleteByPermissionGroupId(String permissionGroupId);

    /**
     * 권한의 모든 매핑 삭제 (권한 삭제 시 사용)
     *
     * @param permissionId 권한 ID
     */
    void deleteByPermissionId(String permissionId);

    /**
     * 특정 그룹-권한 조합 삭제
     *
     * @param permissionGroupId 권한 그룹 ID
     * @param permissionId 권한 ID
     */
    void deleteByPermissionGroupIdAndPermissionId(String permissionGroupId, String permissionId);

    /**
     * 여러 권한 그룹의 모든 권한 ID 조회 (쿼리 최적화)
     *
     * @param permissionGroupIds 권한 그룹 ID 목록
     * @return 권한 ID 리스트
     */
    @Query("SELECT pgp.permissionId FROM PermissionGroupPermissionJpaEntity pgp " +
           "WHERE pgp.permissionGroupId IN :groupIds")
    List<String> findPermissionIdsByGroupIds(@Param("groupIds") Collection<String> permissionGroupIds);
}

