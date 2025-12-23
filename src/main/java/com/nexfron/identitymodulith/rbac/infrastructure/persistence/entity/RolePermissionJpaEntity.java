package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permissions",
                columnNames = {"role_name", "permission_code"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", length = 64, nullable = false)
    private String roleName;

    @Column(name = "permission_code", length = 128, nullable = false)
    private String permissionCode;
}
