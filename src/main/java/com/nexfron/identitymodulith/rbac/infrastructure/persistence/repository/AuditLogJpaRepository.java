package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RBAC 감사 로그 JPA Repository
 *
 * 권한 관리 시스템의 모든 변경 사항을 기록하고 조회합니다.
 *
 * 주요 용도:
 * 1. 감사 로그 기록: 역할/권한 변경 시 자동 기록
 * 2. 변경 이력 조회: 특정 리소스의 모든 변경 추적
 * 3. 사용자 작업 추적: 특정 사용자의 작업 이력 조회
 * 4. 감사(Audit): 규정 준수 및 보안 모니터링
 *
 * 트랜잭션:
 * - 감사 로그는 각 작업 후 자동으로 기록
 * - 감사 로그는 읽기만 가능, 삭제는 관리자만 가능
 *
 * 성능 고려:
 * - timestamp 인덱싱으로 빠른 시간 범위 조회
 * - tenantId 인덱싱으로 멀티테넌시 격리
 * - resourceType 인덱싱으로 리소스별 필터링
 *
 * @see AuditLogJpaEntity
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, String> {

    /**
     * 특정 리소스의 모든 변경 이력을 조회합니다.
     *
     * 사용 시나리오:
     * - "역할 ADMIN의 모든 변경 이력 조회"
     * - "권한 user:manage의 삭제 경위 추적"
     *
     * @param tenantId 테넌트 ID (멀티테넌시 격리)
     * @param resourceType 리소스 타입 (예: "ROLE", "PERMISSION")
     * @param resourceId 리소스 ID (역할 ID, 권한 ID 등)
     * @return 최신순으로 정렬된 감사 로그 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM audit_logs
     *        WHERE tenant_id = ? AND resource_type = ? AND resource_id = ?
     *        ORDER BY timestamp DESC
     */
    List<AuditLogJpaEntity> findByTenantIdAndResourceTypeAndResourceIdOrderByTimestampDesc(
            @Param("tenantId") String tenantId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId
    );

    /**
     * 특정 사용자의 작업 이력을 조회합니다.
     *
     * 사용 시나리오:
     * - "관리자 A가 수행한 모든 작업 조회"
     * - "비정상 권한 변경의 책임자 파악"
     *
     * @param tenantId 테넌트 ID
     * @param operatorId 작업 수행자 ID (사용자 ID)
     * @return 최신순으로 정렬된 감사 로그 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM audit_logs
     *        WHERE tenant_id = ? AND operator_id = ?
     *        ORDER BY timestamp DESC
     */
    List<AuditLogJpaEntity> findByTenantIdAndOperatorIdOrderByTimestampDesc(
            @Param("tenantId") String tenantId,
            @Param("operatorId") String operatorId
    );

    /**
     * 특정 기간의 감사 로그를 조회합니다.
     *
     * 사용 시나리오:
     * - "어제 발생한 모든 권한 변경 조회"
     * - "특정 기간의 감사 로그 내보내기"
     *
     * @param tenantId 테넌트 ID
     * @param startTime 조회 시작 일시 (포함)
     * @param endTime 조회 종료 일시 (포함)
     * @return 시간순으로 정렬된 감사 로그 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM audit_logs
     *        WHERE tenant_id = ? AND timestamp BETWEEN ? AND ?
     *        ORDER BY timestamp ASC
     */
    @Query("""
        SELECT a FROM AuditLogJpaEntity a 
        WHERE a.tenantId = :tenantId 
        AND a.timestamp BETWEEN :startTime AND :endTime 
        ORDER BY a.timestamp ASC
    """)
    List<AuditLogJpaEntity> findByTenantIdAndTimestampBetween(
            @Param("tenantId") String tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 작업 유형의 감사 로그를 조회합니다.
     *
     * 사용 시나리오:
     * - "모든 역할 생성 작업 조회"
     * - "권한 삭제 작업의 이력 확인"
     *
     * @param tenantId 테넌트 ID
     * @param action 작업 유형 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE)
     * @return 최신순으로 정렬된 감사 로그 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM audit_logs
     *        WHERE tenant_id = ? AND action = ?
     *        ORDER BY timestamp DESC
     */
    List<AuditLogJpaEntity> findByTenantIdAndActionOrderByTimestampDesc(
            @Param("tenantId") String tenantId,
            @Param("action") String action
    );

    /**
     * 특정 리소스 타입의 감사 로그를 조회합니다.
     *
     * 사용 시나리오:
     * - "모든 역할(ROLE) 변경 이력 조회"
     * - "사용자-역할(AGENT_ROLE) 할당 이력 조회"
     *
     * @param tenantId 테넌트 ID
     * @param resourceType 리소스 타입 (ROLE, PERMISSION, ROLE_PERMISSION, AGENT_ROLE)
     * @return 최신순으로 정렬된 감사 로그 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM audit_logs
     *        WHERE tenant_id = ? AND resource_type = ?
     *        ORDER BY timestamp DESC
     */
    List<AuditLogJpaEntity> findByTenantIdAndResourceTypeOrderByTimestampDesc(
            @Param("tenantId") String tenantId,
            @Param("resourceType") String resourceType
    );
}

