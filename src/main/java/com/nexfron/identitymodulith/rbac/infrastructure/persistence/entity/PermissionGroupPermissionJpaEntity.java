package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 권한 그룹-권한 매핑 엔티티
 *
 * 목적:
 * - 권한 그룹과 권한의 다대다(N:M) 관계를 표현
 * - 특정 그룹에 어떤 권한들이 포함되는지 관리
 *
 * 관계:
 * PermissionGroup (1) : (N) PermissionGroupPermission (M) : (1) Permission
 *
 * 예시:
 * USER_FULL_ACCESS 그룹:
 *   ├─ user:create
 *   ├─ user:read
 *   ├─ user:update
 *   └─ user:delete
 *
 * 데이터 무결성:
 * - 중복 방지: 같은 그룹-권한 조합은 한 번만 존재
 * - 캐스케이드 삭제: 그룹 삭제 시 매핑 자동 삭제
 * - 캐스케이드 삭제: 권한 삭제 시 매핑 자동 삭제
 *
 * 성능 최적화:
 * - 그룹별 권한 빠른 조회 인덱스
 * - 권한별 그룹 조회 인덱스
 */
@Entity
@Table(
        name = "permission_group_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permission_group_permissions",
                columnNames = {"permission_group_id", "permission_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionGroupPermissionJpaEntity {

    /**
     * 매핑 ID (Primary Key)
     * - Surrogate key (실제 업무 키는 permission_group_id + permission_id 조합)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 권한 그룹 ID (Foreign Key)
     * - permission_groups 테이블 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     */
    @Column(name = "permission_group_id", length = 36, nullable = false)
    private String permissionGroupId;

    /**
     * 권한 ID (Foreign Key)
     * - permissions 테이블 참조
     * - 길이: 36자 (UUID)
     * - 필수값: NOT NULL
     */
    @Column(name = "permission_id", length = 36, nullable = false)
    private String permissionId;

    /**
     * 추가 일시
     * - 권한이 그룹에 추가된 시간
     * - 데이터베이스에서 자동 설정
     * - 수정 불가능
     */
    @Column(name = "added_at", updatable = false, nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}

