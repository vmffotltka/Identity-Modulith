package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 권한 그룹(Permission Group) JPA 엔티티
 *
 * 목적:
 * - 관련된 권한들을 그룹화하여 관리
 * - 역할에 여러 권한을 일일이 할당하는 대신, 그룹 할당으로 간소화
 * - 유지보수 효율성 향상
 *
 * 구조:
 * PermissionGroup (권한 그룹)
 *   ├─ USER_FULL_ACCESS (그룹명)
 *   │  ├─ user:create (권한)
 *   │  ├─ user:read (권한)
 *   │  ├─ user:update (권한)
 *   │  └─ user:delete (권한)
 *   │
 *   └─ ORGANIZATION_ADMIN (그룹명)
 *      ├─ org:create (권한)
 *      ├─ org:read (권한)
 *      ├─ org:update (권한)
 *      └─ org:delete (권한)
 *
 * 사용 흐름:
 * 1. 관리자가 권한 그룹 생성 (예: "USER_FULL_ACCESS")
 * 2. 관리자가 권한들을 그룹에 추가 (user:*, org:view 등)
 * 3. 역할에 그룹 할당
 * 4. 역할에 할당된 모든 권한 자동으로 포함
 *
 * 멀티테넌시:
 * - 각 권한 그룹은 특정 테넌트에 속함
 * - (tenantId, name)으로 유니크 보장
 *
 * 관계:
 * - 1 그룹 : N 권한 (permission_group_permissions 매핑 테이블 통해)
 * - N 역할 : M 그룹 (role_permission_groups 매핑 테이블 통해)
 *
 * 설계 이점:
 * - RBAC 관리 복잡도 감소
 * - 권한 변경 시 영향받는 역할 범위 축소
 * - 권한 정책 재사용성 증가
 */
@Entity
@Table(
        name = "permission_groups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permission_groups_tenant_name",
                columnNames = {"tenant_id", "name"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionGroupJpaEntity {

    /**
     * 권한 그룹 ID (Primary Key)
     * - UUID 형식의 고유 식별자
     * - 예: "550e8400-e29b-41d4-a716-446655440000"
     */
    @Id
    @Column(name = "permission_group_id", length = 36)
    private String permissionGroupId;

    /**
     * 테넌트 ID
     * - 멀티테넌시 환경에서 조직/회사를 구분
     * - 길이: 최대 50자
     * - 필수값: NOT NULL
     */
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    /**
     * 권한 그룹명 (Unique with tenantId)
     * - 사용자가 이해하기 쉬운 형식
     * - 예: "USER_FULL_ACCESS", "ORGANIZATION_ADMIN", "REPORTING_GROUP"
     * - 길이: 최대 64자
     * - 필수값: NOT NULL
     */
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    /**
     * 권한 그룹 설명
     * - 그룹의 목적 및 포함된 권한 범위 설명
     * - 예: "사용자 관리 관련 모든 권한 (생성, 조회, 수정, 삭제)"
     * - 길이: 최대 255자
     * - 선택 사항 (NULL 가능)
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * 활성화 상태
     * - true: 활성 (역할에 할당 가능)
     * - false: 비활성 (더 이상 할당 불가, 기존 할당은 유지)
     * - 기본값: true
     * - 필수값: NOT NULL
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 생성 일시
     * - 권한 그룹이 생성된 정확한 시간
     * - 데이터베이스에서 자동 설정
     * - 수정 불가능 (updatable = false)
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 수정 일시
     * - 권한 그룹이 마지막으로 변경된 시간
     * - 데이터베이스에서 자동 업데이트
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 낙관적 잠금(Optimistic Lock) 버전
     * - JPA에서 동시성 제어를 위해 자동 관리
     * - 그룹 수정 시마다 자동으로 증가
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

