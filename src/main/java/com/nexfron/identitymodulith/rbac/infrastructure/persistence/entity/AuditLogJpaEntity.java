package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RBAC 감사 로그 엔티티
 *
 * <h2>목적:</h2>
 * RBAC(권한 관리) 시스템의 모든 변경 사항을 추적하고 기록합니다.
 * 규정 준수(Compliance), 보안 모니터링, 문제 분석 등에 사용됩니다.
 *
 * <h2>기록 대상:</h2>
 * 1. 역할(Role) 변경
 *    - CREATE: 새 역할 생성
 *    - UPDATE: 역할 정보 수정
 *    - DELETE: 역할 삭제
 *
 * 2. 권한(Permission) 변경
 *    - CREATE: 새 권한 생성
 *    - UPDATE: 권한 정보 수정
 *    - DELETE: 권한 삭제
 *
 * 3. 역할-권한 관계
 *    - ASSIGN: 역할에 권한 할당
 *    - REVOKE: 역할에서 권한 회수
 *
 * 4. 사용자-역할 관계
 *    - ASSIGN: 사용자에게 역할 할당
 *    - REVOKE: 사용자에게서 역할 회수
 *
 * <h2>데이터 구조:</h2>
 * - auditId: 감사 로그 고유 ID (UUID)
 * - tenantId: 멀티테넌시 격리 (필수)
 * - action: 수행된 작업 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE)
 * - resourceType: 대상 리소스 타입 (ROLE, PERMISSION, ROLE_PERMISSION, AGENT_ROLE)
 * - resourceId: 대상 리소스 ID (역할 ID, 권한 ID 등)
 * - operatorId: 작업 수행자 ID (사용자 ID)
 * - changes: 변경 내용 (JSON 형식)
 * - timestamp: 작업 수행 일시
 *
 * <h2>사용 예시:</h2>
 * {@code
 * // 역할 생성 감사 로그
 * AuditLogJpaEntity audit = AuditLogJpaEntity.builder()
 *     .auditId(UUID.randomUUID().toString())
 *     .tenantId("tenant-001")
 *     .action("CREATE")
 *     .resourceType("ROLE")
 *     .resourceId(roleId)
 *     .operatorId(currentUserId)
 *     .changes("{\"name\": \"NEW_ROLE\", \"type\": \"POSITION\"}")
 *     .timestamp(LocalDateTime.now())
 *     .build();
 * }
 *
 * <h2>쿼리 예시:</h2>
 * 1. 특정 역할의 모든 변경 이력
 *    SELECT * FROM audit_logs WHERE resourceType = 'ROLE' AND resourceId = ?
 *
 * 2. 특정 사용자의 작업 추적
 *    SELECT * FROM audit_logs WHERE operatorId = ? ORDER BY timestamp DESC
 *
 * 3. 특정 기간의 권한 변경
 *    SELECT * FROM audit_logs
 *    WHERE resourceType IN ('ROLE', 'PERMISSION')
 *    AND timestamp BETWEEN ? AND ?
 *    ORDER BY timestamp DESC
 *
 * <h2>보관 정책:</h2>
 * - 기간: 최소 1년 이상 보관
 * - 삭제: 정책에 따라 주기적으로 아카이빙
 * - 접근 제어: 감사 로그는 읽기 전용, 수정 불가
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_audit_logs_resource_type", columnList = "resource_type"),
                @Index(name = "idx_audit_logs_operator_id", columnList = "operator_id"),
                @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_logs_tenant_timestamp", columnList = "tenant_id,timestamp")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogJpaEntity {

    /**
     * 감사 로그 고유 ID (UUID)
     */
    @Id
    @Column(name = "audit_id", length = 36, nullable = false)
    private String auditId;

    /**
     * 테넌트 ID (멀티테넌시 격리, 필수)
     * - 멀티테넌시 환경에서 테넌트별 감사 로그 격리
     * - 감사 데이터 보안 및 프라이버시 보호
     */
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    /**
     * 수행된 작업 (Action)
     *
     * 가능한 값:
     * - "CREATE": 새로운 리소스 생성
     * - "UPDATE": 기존 리소스 정보 수정
     * - "DELETE": 리소스 삭제
     * - "ASSIGN": 권한/역할 할당
     * - "REVOKE": 권한/역할 회수
     *
     * 표준화된 영문 대문자 사용
     */
    @Column(name = "action", length = 32, nullable = false)
    private String action;

    /**
     * 대상 리소스 타입
     *
     * 가능한 값:
     * - "ROLE": 역할(Role) 엔티티
     * - "PERMISSION": 권한(Permission) 엔티티
     * - "ROLE_PERMISSION": 역할-권한 매핑
     * - "AGENT_ROLE": 사용자-역할 매핑
     *
     * 조회 필터링에 사용되므로 표준화 필수
     */
    @Column(name = "resource_type", length = 32, nullable = false)
    private String resourceType;

    /**
     * 대상 리소스 ID
     *
     * - resourceType이 ROLE이면 역할 ID
     * - resourceType이 PERMISSION이면 권한 ID
     * - resourceType이 ROLE_PERMISSION이면 역할-권한 매핑 ID 또는 역할 ID
     * - resourceType이 AGENT_ROLE이면 사용자 ID 또는 역할 ID
     *
     * 조회 및 추적용 필드
     */
    @Column(name = "resource_id", length = 36, nullable = false)
    private String resourceId;

    /**
     * 작업 수행자 ID (Operator)
     *
     * - 해당 작업을 수행한 사용자의 ID (에이전트 ID)
     * - null 가능: 시스템이 자동으로 수행한 경우
     *
     * 용도:
     * - 작업 주체 추적
     * - 비정상 접근 시도 감지
     * - 사용자별 작업 히스토리 조회
     */
    @Column(name = "operator_id", length = 36)
    private String operatorId;

    /**
     * 변경 내용 (상세 정보, JSON 형식)
     *
     * 예시:
     * 1. 역할 생성: {"name": "NEW_ROLE", "type": "POSITION"}
     * 2. 권한 생성: {"code": "user:manage", "description": "사용자 관리 권한"}
     * 3. 역할-권한 할당: {"roleId": "role-001", "permissionId": "perm-001"}
     * 4. 사용자-역할 할당: {"agentId": "user-123", "roleName": "TEAM_LEADER"}
     * 5. 정보 변경 (UPDATE): {"old": {"field": "oldValue"}, "new": {"field": "newValue"}}
     *
     * JSON 저장으로 유연한 확장성 제공
     */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    /**
     * 작업 수행 일시
     *
     * - 작업이 완료된 시각 (ms 단위 정밀도)
     * - UTC 또는 시스템 타임존으로 통일
     *
     * 용도:
     * - 변경 시점 추적
     * - 시간 범위 조회 (e.g., 어제의 모든 권한 변경)
     * - 이벤트 순서 파악
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * 추가 정보 (선택사항)
     *
     * 용도:
     * - 작업 실패 원인 기록
     * - 성공 여부 플래그
     * - 관리자 메모
     */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    /**
     * IP 주소 (선택사항)
     *
     * 용도:
     * - 비정상 접근 추적
     * - 지리적 접근 분석
     * - 보안 감시
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}

