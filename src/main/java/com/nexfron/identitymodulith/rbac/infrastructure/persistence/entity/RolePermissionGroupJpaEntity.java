package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 역할-권한 그룹 매핑 엔티티
 *
 * 목적:
 * - 역할과 권한 그룹의 다대다(N:M) 관계를 표현
 * - 특정 역할에 어떤 권한 그룹들이 할당되었는지 관리
 *
 * 관계:
 * Role (1) : (N) RolePermissionGroup (M) : (1) PermissionGroup
 *
 * 데이터 흐름:
 * Role → RolePermissionGroup → PermissionGroup → PermissionGroupPermission → Permission
 *
 * 예시:
 * TEAM_LEADER 역할:
 *   ├─ USER_FULL_ACCESS 그룹 (user:create, user:read, user:update, user:delete)
 *   ├─ ORGANIZATION_READ 그룹 (org:view)
 *   └─ REPORTING_GROUP 그룹 (report:view, report:export)
 *
 * 장점:
 * - 역할에 개별 권한을 할당하지 않고 그룹으로 관리
 * - 권한 정책 변경 시 그룹 수정으로 모든 역할에 반영
 * - 관리 복잡도 감소
 *
 * 데이터 무결성:
 * - 중복 방지: 같은 역할-그룹 조합은 한 번만 존재
 * - 캐스케이드 삭제: 역할 삭제 시 매핑 자동 삭제
 * - 캐스케이드 삭제: 그룹 삭제 시 매핑 자동 삭제
 */
@Entity
@Table(
        name = "role_permission_groups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permission_groups",
                columnNames = {"role_id", "permission_group_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionGroupJpaEntity {

    /**
     * 매핑 ID (Primary Key)
     * - Surrogate key (실제 업무 키는 role_id + permission_group_id 조합)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 역할 ID (Foreign Key)
     * - roles 테이블 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     */
    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    /**
     * 권한 그룹 ID (Foreign Key)
     * - permission_groups 테이블 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     */
    @Column(name = "permission_group_id", length = 36, nullable = false)
    private String permissionGroupId;

    /**
     * 할당 일시
     * - 권한 그룹이 역할에 할당된 시간
     * - 데이터베이스에서 자동 설정
     * - 수정 불가능
     */
    @Column(name = "assigned_at", updatable = false, nullable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}

