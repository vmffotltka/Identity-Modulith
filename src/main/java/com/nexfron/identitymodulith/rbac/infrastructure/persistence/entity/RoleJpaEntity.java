package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 역할(Role) JPA 엔티티
 *
 * RBAC(Role-Based Access Control) 시스템에서 권한의 집합을 나타냅니다.
 * 시스템에서 사용자에게 부여할 수 있는 역할을 정의하며,
 * 각 역할은 다양한 권한(Permission)을 가질 수 있습니다.
 *
 * 역할은 조직의 직책, 채널, 능력 등 여러 차원으로 분류될 수 있으며,
 * 역할이 변경되면 해당 역할을 가진 모든 사용자에게 자동으로 반영됩니다.
 *
 * 데이터 흐름:
 * Role (역할) → RolePermission (매핑) → Permission (권한)
 *                ↑
 *         AgentRole (매핑)
 *                ↑
 *            Agent (사용자)
 *
 * 예시 구조:
 *
 * ADMIN 역할 (POSITION 타입)
 * ├─ user:manage (권한) - 사용자 관리
 * ├─ org:manage (권한) - 조직 관리
 * ├─ report:export (권한) - 보고서 내보내기
 * └─ 할당된 사용자: [홍길동, 김철수]
 *
 * TEAM_LEADER 역할 (POSITION 타입)
 * ├─ team:manage (권한) - 팀 관리
 * ├─ report:view (권한) - 보고서 조회
 * └─ 할당된 사용자: [박영희, 이순신]
 *
 * PHONE_AGENT 역할 (CHANNEL 타입)
 * ├─ call:accept (권한) - 전화 수신
 * ├─ call:transfer (권한) - 전화 전달
 * └─ 할당된 사용자: [김영희, 이미영, 박수진]
 *
 * 멀티테넌시 지원:
 * - 각 역할은 특정 테넌트(조직/회사)에 속함
 * - 같은 이름의 역할이라도 테넌트별로 독립적으로 관리됨
 * - (tenantId, name)의 조합으로 유니크 보장 (중복 역할 방지)
 * - 예: 회사A의 ADMIN과 회사B의 ADMIN은 서로 다름
 *
 * 관계:
 * - 1 역할 : N 권한 (role_permissions 테이블을 통해)
 * - 1 역할 : N 사용자 (agent_roles 테이블을 통해)
 *
 * 권장 사항:
 * - 역할 이름은 대문자 영문으로 작성 (ADMIN, TEAM_LEADER, MEMBER)
 * - 역할 타입은 미리 정의된 타입으로 관리 (POSITION, CHANNEL, SKILL)
 * - 권한 변경 시 영향받는 사용자 수를 먼저 확인
 */
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roles_tenant_name",
                columnNames = {"tenant_id", "name"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoleJpaEntity {

    /**
     * 역할 ID (Primary Key)
     * - UUID 형식의 고유 식별자
     * - 데이터베이스 전체에서 유일성 보장
     * - 예: "550e8400-e29b-41d4-a716-446655440000"
     */
    @Id
    @Column(name = "role_id", length = 36)
    private String roleId;

    /**
     * 테넌트 ID (Foreign Key)
     * - 멀티테넌시 환경에서 조직/회사를 구분
     * - 같은 역할명이라도 테넌트별로 독립적으로 관리
     * - 길이: 최대 50자
     * - 필수값: NOT NULL
     * - 예: "tenant-001", "company-xyz"
     */
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    /**
     * 역할명 (Unique with tenantId)
     * - 테넌트 내에서 고유한 역할 이름
     * - 사용자가 이해하기 쉬운 형식 (예: ADMIN, TEAM_LEADER, MEMBER)
     * - 길이: 최대 64자
     * - 필수값: NOT NULL
     * - 중복 방지: tenantId와 함께 UNIQUE 제약
     */
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    /**
     * 역할의 타입 (POSITION / CHANNEL / SKILL)
     * 역할을 분류하여 관리 및 조회 효율성 증대:
     *
     * - POSITION: 직급 관련 역할
     *   예: ADMIN, MANAGER, TEAM_LEADER, MEMBER
     *
     * - CHANNEL: 채널(통신 수단) 관련 역할
     *   예: PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT
     *
     * - SKILL: 전문 능력 관련 역할
     *   예: TECHNICAL_SUPPORT, BILLING_EXPERT, VIP_SPECIALIST
     *
     * 길이: 최대 32자
     * 필수값: NOT NULL
     */
    @Column(name = "type", length = 32, nullable = false)
    private String type;

    /**
     * 생성 일시
     * - 역할이 생성된 정확한 시간
     * - 데이터베이스에서 자동 설정 (현재 시간)
     * - 수정 불가능 (updatable = false)
     * - 감시 추적(Audit Trail)용으로 사용
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 수정 일시
     * - 역할의 권한이나 설정이 마지막으로 변경된 시간
     * - 데이터베이스에서 자동 업데이트 (수정 시)
     * - 데이터 일관성 및 변경 추적용
     * - Keycloak 동기화 시간 판단에 활용 가능
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
