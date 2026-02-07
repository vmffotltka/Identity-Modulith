package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 역할-권한 매핑 JPA 엔티티
 *
 * RBAC(Role-Based Access Control) 시스템에서 역할(Role)과 권한(Permission)의
 * 다대다(Many-to-Many) 관계를 정의합니다.
 * 한 역할은 여러 권한을 가질 수 있으며,
 * 한 권한도 여러 역할에 할당될 수 있습니다.
 *
 * 이 테이블은 순수 매핑 전용이므로 비즈니스 로직이 포함되지 않습니다.
 *
 * 데이터 흐름:
 * Permission (권한) ←─N:M─→ RolePermission (매핑) ←─N:M─→ Role (역할)
 *                                                              ↑
 *                                                            N:M
 *                                                              ↑
 *                                                        AgentRole (매핑)
 *                                                              ↑
 *                                                              N
 *                                                              ↑
 *                                                          Agent (사용자)
 *
 * 예시:
 * - ADMIN 역할에 "user:manage", "org:view", "report:export" 권한 할당
 * - TEAM_LEADER 역할에 "team:manage", "report:view" 권한 할당
 * - "report:view" 권한은 ADMIN, TEAM_LEADER 두 역할에 모두 할당됨 (다대다)
 * - 역할 변경 시 자동으로 해당 역할의 모든 사용자 권한이 반영됨
 *
 * 특징:
 * - (roleId, permissionId)의 조합으로 유니크 보장 (중복 할당 방지)
 * - 동일한 역할-권한 조합의 중복 제거됨 (UNIQUE 제약)
 * - Surrogate Key (id)로 데이터베이스 성능 최적화
 * - 카스케이드 삭제: 역할 또는 권한 삭제 시 매핑도 자동 삭제 (FK ON DELETE CASCADE)
 */
@Entity
@Table(
        name = "rbac_role_permissions",  // V1_0_20: 표준 명명 규칙 적용 (role_permissions → rbac_role_permissions)
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permissions",
                columnNames = {"role_id", "permission_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionJpaEntity {

    /**
     * 매핑 ID (Primary Key, Surrogate Key)
     * - BIGSERIAL: 자동 증가 정수형 (1, 2, 3, ...)
     * - 데이터베이스 조인 성능 최적화를 위해 사용
     * - 비즈니스적 의미는 없음 (FK는 (roleId, permissionId) 조합으로 처리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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
     * 권한 ID (Foreign Key to permissions table)
     * - UUID 형식의 권한 식별자
     * - permissions 테이블의 permission_id를 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     * - 카스케이드 삭제: 권한 삭제 시 이 매핑도 삭제됨
     *
     * 데이터 예시:
     * - "550e8400-e29b-41d4-a716-446655440010" (user:manage 권한)
     * - "550e8400-e29b-41d4-a716-446655440011" (org:view 권한)
     */
    @Column(name = "permission_id", length = 36, nullable = false)
    private String permissionId;

    /**
     * 할당 일시
     * - 이 권한이 역할에 부여된 정확한 시간
     * - 데이터베이스에서 자동 설정 (현재 시간)
     * - 수정 불가능 (updatable = false)
     * - 감시 추적(Audit Trail)용으로 사용
     * - 권한 변경 이력 추적에 활용 가능
     */
    @Column(name = "assigned_at", updatable = false, nullable = false)
    private LocalDateTime assignedAt;

    /**
     * 할당 해제 일시 (선택적 - 미래 확장용)
     * - 이 권한이 역할에서 제거된 시간 (Soft Delete)
     * - NULL: 현재 활성 상태의 권한
     * - NOT NULL: 제거된 권한 (역사 추적용)
     * - 미래 확장: 권한 제거 이력이 필요한 경우 사용
     *
     * 현재 미사용이지만, 규정 준수(Audit Trail) 필요 시 추가 가능
     */
    // @Column(name = "unassigned_at")
    // private LocalDateTime unassignedAt;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
