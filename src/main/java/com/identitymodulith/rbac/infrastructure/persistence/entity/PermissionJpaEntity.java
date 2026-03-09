package com.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 권한(Permission) JPA 엔티티
 *
 * RBAC(Role-Based Access Control) 시스템에서 세분화된 접근 제어를 정의합니다.
 * 시스템에서 정의할 수 있는 모든 권한을 나타내며,
 * 권한은 역할(Role)에 할당되고, 역할이 사용자에게 부여됩니다.
 *
 * 권한 구조:
 * Permission (권한) → RolePermission (매핑) → Role (역할) → AgentRole (매핑) → Agent (사용자)
 *
 * 예시:
 *
 * 권한 코드: "user:manage"
 * ├─ 설명: 사용자 계정 생성, 수정, 삭제 및 관리
 * ├─ 도메인: user (사용자 관련)
 * ├─ 액션: manage (생성/수정/삭제 등 관리)
 * ├─ 포함 역할: [ADMIN, HR_MANAGER]
 * └─ 영향 사용자: [홍길동, 김철수]
 *
 * 권한 코드: "org:view"
 * ├─ 설명: 조직 구조 조회
 * ├─ 도메인: org (조직 관련)
 * ├─ 액션: view (조회 전용)
 * ├─ 포함 역할: [ADMIN, TEAM_LEADER, MEMBER]
 * └─ 영향 사용자: [전체 사용자]
 *
 * 권장 명명 규칙 (domain:action):
 *
 * Domain 예시:
 * - user: 사용자 관리
 * - org: 조직/부서 관리
 * - report: 보고서 관리
 * - task: 업무 관리
 * - channel: 채널/통신 관리
 * - permission: 권한 관리
 *
 * Action 예시:
 * - view: 조회 전용
 * - create: 생성
 * - update: 수정
 * - delete: 삭제
 * - manage: 생성/수정/삭제 모두
 * - export: 내보내기
 * - import: 가져오기
 *
 * 멀티테넌시 지원:
 * - 각 권한은 특정 테넌트(조직/회사)에 속함
 * - 같은 권한 코드라도 테넌트별로 독립적으로 관리됨
 * - (tenantId, code)의 조합으로 유니크 보장 (중복 권한 방지)
 * - 예: 회사A의 "user:manage"와 회사B의 "user:manage"는 서로 다름
 *
 * 권한 변경의 영향:
 * - 권한 삭제 시 해당 권한을 가진 모든 역할이 영향을 받음
 * - 모든 역할이 영향을 받으면 해당 역할의 모든 사용자가 영향을 받음
 * - 권한 추가/수정은 기존 데이터에 영향 없음
 *
 * 관계:
 * - 1 권한 : N 역할 (role_permissions 테이블을 통해)
 *
 * 권장 사항:
 * - 권한은 이미 정의된 것만 사용 (임의로 생성하지 말 것)
 * - 권한 코드는 변경하지 말 것 (코드에 의존하는 모든 부분에 영향)
 * - 새로운 권한이 필요하면 요청 후 승인받아 추가
 * - 권한 설명은 명확하고 구체적으로 작성
 */
@Entity
@Table(
        name = "rbac_permissions",  // V1_0_20: 표준 명명 규칙 적용 (permissions → rbac_permissions)
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permissions_tenant_code",
                columnNames = {"tenant_id", "code"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionJpaEntity {

    /**
     * 권한 ID (Primary Key)
     * - UUID 형식의 고유 식별자
     * - 데이터베이스 전체에서 유일성 보장
     * - 예: "550e8400-e29b-41d4-a716-446655440001"
     */
    @Id
    @Column(name = "permission_id", length = 36)
    private String permissionId;

    /**
     * 테넌트 ID (Foreign Key)
     * - 멀티테넌시 환경에서 조직/회사를 구분
     * - 같은 권한 코드라도 테넌트별로 독립적으로 관리
     * - 길이: 최대 50자
     * - 필수값: NOT NULL
     * - 예: "tenant-001", "company-xyz"
     */
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    /**
     * 권한 코드 (Unique with tenantId)
     * - 테넌트 내에서 고유한 권한 코드
     * - 형식: "domain:action" (예: "user:manage", "org:view")
     * - 프로그래매틱 식별자로 사용 (UI와 API에서 참조)
     * - 길이: 최대 128자
     * - 필수값: NOT NULL
     * - 중복 방지: tenantId와 함께 UNIQUE 제약
     *
     * 권장 명명 규칙:
     * - domain: user, org, report, task, channel 등
     * - action: view, create, update, delete, manage, export 등
     */
    @Column(name = "code", length = 128, nullable = false)
    private String code;

    /**
     * 권한명
     * - 사용자 친화적인 권한 이름
     * - UI에서 표시되는 이름
     * - 길이: 최대 100자
     * - 필수값: NOT NULL
     *
     * 예시:
     * - "사용자 관리"
     * - "대시보드 조회"
     * - "보고서 내보내기"
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * 권한 설명
     * - 관리자가 이해할 수 있는 권한의 목적 및 기능 설명
     * - 권한 관리 UI에서 사용자 친화적 정보 제공
     * - 길이: 최대 500자
     * - 선택값: NULL 허용 (권한 코드만으로 이해 가능한 경우)
     *
     * 예시:
     * - "사용자 계정 생성, 수정, 삭제 및 전체 관리 권한"
     * - "조직 구조 트리 및 부서 정보 조회만 가능 (수정 불가)"
     * - "보고서를 PDF, Excel 형식으로 내보내기"
     * - "모든 유형의 작업 생성 및 할당 가능"
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 권한 카테고리 (도메인 그룹)
     * - 권한을 도메인별로 그룹화하여 관리 및 조회 용이성 향상
     * - RP-001: 권한 카테고리별 그룹화 구현
     * - 길이: 최대 64자
     * - 선택값: NULL 허용 (기본 카테고리: GENERAL)
     *
     * 카테고리 예시:
     * - USER_MANAGEMENT: 사용자 계정 관리 (user:create, user:update, user:delete)
     * - ORG_MANAGEMENT: 조직/부서 관리 (org:create, org:update, dept:manage)
     * - REPORT: 보고서 관련 (report:view, report:export, report:create)
     * - TASK: 업무 관리 (task:create, task:assign, task:complete)
     * - CHANNEL: 채널/통신 관리 (channel:view, channel:message, channel:admin)
     * - PERMISSION_ADMIN: 권한 관리 (permission:manage, role:create)
     * - GENERAL: 일반 권한 (기본 카테고리)
     *
     * 활용:
     * - 권한 목록 UI에서 카테고리별 필터링
     * - 역할 생성 시 카테고리별 권한 그룹 선택
     * - 감사 로그에서 카테고리별 권한 변경 추적
     */
    @Column(name = "category", length = 64)
    private String category;

    /**
     * 리소스
     * - 권한이 적용되는 리소스 (예: dashboard, user, report)
     * - 길이: 최대 100자
     * - 선택값: NULL 허용
     */
    @Column(name = "resource", length = 100)
    private String resource;

    /**
     * 액션
     * - 권한이 수행하는 동작 (예: view, create, update, delete)
     * - 길이: 최대 50자
     * - 선택값: NULL 허용
     */
    @Column(name = "action", length = 50)
    private String action;

    /**
     * 생성 일시
     * - 권한이 생성된 정확한 시간
     * - 데이터베이스에서 자동 설정 (현재 시간)
     * - 수정 불가능 (updatable = false)
     * - 감시 추적(Audit Trail)용으로 사용
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 버전 (Optimistic Locking)
     * - 동시성 제어를 위한 낙관적 락
     * - 동시에 여러 트랜잭션이 같은 권한을 수정하려 할 때 충돌 방지
     * - 업데이트할 때마다 자동으로 증가
     * - 충돌 발생 시 OptimisticLockException 발생
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 마지막 수정 일시
     * - 권한의 설명이나 설정이 마지막으로 변경된 시간
     * - 데이터베이스에서 자동 업데이트 (수정 시)
     * - 데이터 일관성 및 변경 추적용
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
