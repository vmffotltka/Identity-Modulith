package com.identitymodulith.organization.infrastructure.persistence.repository;

import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 부서 조회/검색 리포지토리 (테넌트 격리 기준). */
public interface JpaDepartmentRepository extends JpaRepository<DepartmentEntity, String> {

    @Query("""
        SELECT d.deptId AS deptId,
               d.name AS name,
               d.type AS type,
               d.orgPath AS orgPath,
               d.depth AS depth,
               p.deptId AS parentId,
               d.status AS status
        FROM DepartmentEntity d
        LEFT JOIN d.parent p
        WHERE d.tenantId = :tenantId
    """)
    List<DepartmentListProjection> findAllProjectedByTenantId(@Param("tenantId") String tenantId);

    @Query("""
        SELECT d.deptId AS deptId,
               d.name AS name,
               d.type AS type,
               d.orgPath AS orgPath,
               d.depth AS depth,
               p.deptId AS parentId,
               d.status AS status
        FROM DepartmentEntity d
        LEFT JOIN d.parent p
        WHERE d.tenantId = :tenantId
          AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<DepartmentListProjection> findProjectedByTenantIdAndNameContainingIgnoreCase(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword);

    @Query("""
        SELECT d.deptId AS deptId,
               d.name AS name,
               d.type AS type,
               d.orgPath AS orgPath,
               d.depth AS depth,
               p.deptId AS parentId,
               d.status AS status
        FROM DepartmentEntity d
        LEFT JOIN d.parent p
        WHERE d.tenantId = :tenantId
          AND d.orgPath LIKE CONCAT(:orgPathPrefix, '%')
    """)
    List<DepartmentListProjection> findProjectedByTenantIdAndOrgPathStartsWith(
            @Param("tenantId") String tenantId,
            @Param("orgPathPrefix") String orgPathPrefix);

    @Query("""
        SELECT d.deptId AS deptId,
               d.name AS name,
               d.type AS type,
               d.orgPath AS orgPath,
               d.depth AS depth,
               p.deptId AS parentId,
               d.status AS status
        FROM DepartmentEntity d
        LEFT JOIN d.parent p
        WHERE d.tenantId = :tenantId
          AND d.depth = :depth
    """)
    List<DepartmentListProjection> findProjectedByTenantIdAndDepth(
            @Param("tenantId") String tenantId,
            @Param("depth") Integer depth);

    @Query("""
        SELECT d.deptId AS deptId,
               d.name AS name,
               d.type AS type,
               d.orgPath AS orgPath,
               d.depth AS depth,
               p.deptId AS parentId,
               d.status AS status
        FROM DepartmentEntity d
        LEFT JOIN d.parent p
        WHERE d.tenantId = :tenantId
          AND d.type = :type
    """)
    List<DepartmentListProjection> findProjectedByTenantIdAndType(
            @Param("tenantId") String tenantId,
            @Param("type") DepartmentType type);

    @Query("""
        SELECT d.deptId AS deptId,
               d.name AS name,
               d.type AS type,
               d.orgPath AS orgPath,
               d.depth AS depth,
               p.deptId AS parentId,
               d.status AS status
        FROM DepartmentEntity d
        LEFT JOIN d.parent p
        WHERE d.tenantId = :tenantId
          AND d.deptId IN :deptIds
    """)
    List<DepartmentListProjection> findProjectedByTenantIdAndDeptIdIn(
            @Param("tenantId") String tenantId,
            @Param("deptIds") Set<String> deptIds);

    List<DepartmentEntity> findByTenantIdAndOrgPathStartsWith(String tenantId, String orgPathPrefix);

    /**
     * parent를 Fetch Join으로 함께 조회해 parent 접근 시 N+1을 줄인다.
     */
    @Query("""
        SELECT d FROM DepartmentEntity d
        LEFT JOIN FETCH d.parent
        WHERE d.tenantId = :tenantId
          AND d.orgPath LIKE CONCAT(:orgPathPrefix, '%')
    """)
    List<DepartmentEntity> findByTenantIdAndOrgPathStartsWithWithParent(
            @Param("tenantId") String tenantId,
            @Param("orgPathPrefix") String orgPathPrefix);

    List<DepartmentEntity> findAllByTenantId(String tenantId);

    /**
     * 조직도 트리 조회용 parent Fetch Join.
     */
    @Query("""
        SELECT d FROM DepartmentEntity d
        LEFT JOIN FETCH d.parent
        WHERE d.tenantId = :tenantId
    """)
    List<DepartmentEntity> findAllByTenantIdWithParent(@Param("tenantId") String tenantId);

    boolean existsByParent(DepartmentEntity parent);

    Optional<DepartmentEntity> findByDeptIdAndTenantId(String deptId, String tenantId);

    /** 단건 조회 + parent Fetch Join. */
    @Query("""
        SELECT d FROM DepartmentEntity d
        LEFT JOIN FETCH d.parent
        WHERE d.deptId = :deptId
          AND d.tenantId = :tenantId
    """)
    Optional<DepartmentEntity> findByDeptIdAndTenantIdWithParent(
            @Param("deptId") String deptId,
            @Param("tenantId") String tenantId);

    List<DepartmentEntity> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String keyword);

    /** 부서명 검색 + parent Fetch Join. */
    @Query("""
        SELECT d FROM DepartmentEntity d
        LEFT JOIN FETCH d.parent
        WHERE d.tenantId = :tenantId
          AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<DepartmentEntity> findByTenantIdAndNameContainingIgnoreCaseWithParent(
            @Param("tenantId") String tenantId,
            @Param("keyword") String keyword);

    List<DepartmentEntity> findByTenantIdAndDepth(String tenantId, Integer depth);

    /** 깊이 조회 + parent Fetch Join. */
    @Query("""
        SELECT d FROM DepartmentEntity d
        LEFT JOIN FETCH d.parent
        WHERE d.tenantId = :tenantId
          AND d.depth = :depth
    """)
    List<DepartmentEntity> findByTenantIdAndDepthWithParent(
            @Param("tenantId") String tenantId,
            @Param("depth") Integer depth);

    List<DepartmentEntity> findByTenantIdAndType(String tenantId, DepartmentType type);

    /** 타입 조회 + parent Fetch Join. */
    @Query("""
        SELECT d FROM DepartmentEntity d
        LEFT JOIN FETCH d.parent
        WHERE d.tenantId = :tenantId
          AND d.type = :type
    """)
    List<DepartmentEntity> findByTenantIdAndTypeWithParent(
            @Param("tenantId") String tenantId,
            @Param("type") DepartmentType type);
}
