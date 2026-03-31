package com.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 테넌트별 권한 메타데이터 엔티티. */
@Entity
@Table(
        name = "rbac_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_permissions_tenant_code",
                columnNames = {"tenant_id", "code"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionJpaEntity {

    @Id
    @Column(name = "permission_id", length = 36)
    private String permissionId;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "code", length = 128, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "resource", length = 100)
    private String resource;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

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
