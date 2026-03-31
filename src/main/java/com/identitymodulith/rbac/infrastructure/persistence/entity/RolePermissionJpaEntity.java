package com.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 역할-권한 매핑 엔티티. */
@Entity
@Table(
        name = "rbac_role_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permissions",
                columnNames = {"role_id", "permission_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    @Column(name = "permission_id", length = 36, nullable = false)
    private String permissionId;

    @Column(name = "assigned_at", updatable = false, nullable = false)
    private LocalDateTime assignedAt;


    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
