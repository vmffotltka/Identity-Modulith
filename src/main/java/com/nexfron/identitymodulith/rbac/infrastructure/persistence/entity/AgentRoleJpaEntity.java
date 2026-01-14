package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 에이전트(Agent)-역할(Role) 매핑 JPA 엔티티
 *
 * RBAC(Role-Based Access Control) 시스템에서 사용자(에이전트)와 역할(Role)의
 * 다대다(Many-to-Many) 관계를 정의합니다.
 * 한 사용자는 여러 역할을 가질 수 있으며,
 * 한 역할도 여러 사용자에게 부여될 수 있습니다.
 *
 * 데이터 흐름:
 * Agent (사용자) → AgentRole (매핑) → Role (역할) → RolePermission (매핑) → Permission (권한)
 *
 * 예시 구조:
 * Agent: "user123" (사용자 홍길동)
 *   ├─ ADMIN (역할)
 *   │   ├─ user:manage (권한)
 *   │   ├─ org:manage (권한)
 *   │   └─ report:export (권한)
 *   └─ TEAM_LEADER (역할)
 *       ├─ team:manage (권한)
 *       └─ report:view (권한)
 *
 * 특징:
 * - (agentId, roleId)의 조합으로 유니크 보장 → 중복 역할 할당 방지
 * - Surrogate Key (id) 사용으로 데이터베이스 성능 최적화
 * - User 모듈의 agents 테이블과 RBAC 모듈의 roles 테이블을 연결
 * - 역할 할당/회수 이력 추적 가능 (assigned_at으로 감시 추적)
 *
 * 관계 다이어그램:
 * agents (User Module) ──1:N── agent_roles ──N:1── roles (RBAC Module)
 *                                                      │
 *                                                    1:N
 *                                                      │
 *                                                 role_permissions
 *                                                      │
 *                                                    N:1
 *                                                      │
 *                                                 permissions
 */
@Entity
@Table(
        name = "agent_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agent_roles",
                columnNames = {"agent_id", "role_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AgentRoleJpaEntity {

    /**
     * 매핑 ID (Primary Key, Surrogate Key)
     * - BIGSERIAL: 자동 증가 정수형 (1, 2, 3, ...)
     * - 데이터베이스 조인 성능 최적화를 위해 사용
     * - 비즈니스적 의미는 없음 (실제 매핑은 (agentId, roleId) 조합으로 처리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 에이전트 ID (Foreign Key to agents table in User Module)
     * - UUID 형식의 사용자 식별자
     * - agents 테이블의 agent_id를 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     * - 카스케이드 삭제: 사용자 삭제 시 이 매핑도 삭제됨
     *
     * 데이터 예시:
     * - "550e8400-e29b-41d4-a716-446655440100" (사용자 홍길동)
     * - "550e8400-e29b-41d4-a716-446655440101" (사용자 김철수)
     */
    @Column(name = "agent_id", length = 36, nullable = false)
    private String agentId;

    /**
     * 역할 ID (Foreign Key to roles table)
     * - UUID 형식의 역할 식별자
     * - roles 테이블의 role_id를 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     * - 카스케이드 삭제: 역할 삭제 시 이 매핑도 삭제됨
     *
     * 데이터 예시:
     * - "550e8400-e29b-41d4-a716-446655440000" (ADMIN 역할)
     * - "550e8400-e29b-41d4-a716-446655440001" (TEAM_LEADER 역할)
     */
    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    /**
     * 할당 일시
     * - 이 역할이 에이전트에게 부여된 정확한 시간
     * - 데이터베이스에서 자동 설정 (현재 시간)
     * - 수정 불가능 (updatable = false)
     * - 감시 추적(Audit Trail)용으로 사용
     * - 역할 변경 이력 추적에 활용 가능
     *
     * 활용 예시:
     * - "언제부터 이 사용자가 팀리더 역할을 수행했는가?"
     * - "지난 3개월 내에 권한이 변경된 사용자는?"
     */
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}

