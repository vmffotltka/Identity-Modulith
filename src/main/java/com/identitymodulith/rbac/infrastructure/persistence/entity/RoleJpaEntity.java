package com.identitymodulith.rbac.infrastructure.persistence.entity;

import com.identitymodulith.common.domain.DataScopeLevel;
import com.identitymodulith.rbac.domain.RoleType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** RBAC 역할 메타데이터 엔티티. */
@Entity
@Table(
        name = "rbac_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roles_tenant_name",
                columnNames = {"tenant_id", "name"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoleJpaEntity {

    @Id
    @Column(name = "role_id", length = 36)
    private String roleId;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false)
    private RoleType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope_level", length = 32)
    private DataScopeLevel dataScope;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

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
