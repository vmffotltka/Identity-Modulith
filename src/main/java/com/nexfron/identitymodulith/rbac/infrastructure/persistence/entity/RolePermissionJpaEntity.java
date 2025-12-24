package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 역할-권한 매핑 JPA 엔티티
 *
 * 역할(Role)과 권한(Permission)의 관계를 정의합니다.
 * 한 역할은 여러 권한을 가질 수 있습니다.
 *
 * 예: ADMIN 역할 -> "user:manage", "org:view", "report:export" 권한
 */
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

    /**
     * 매핑 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 역할명 (Foreign Key to roles table)
     */
    @Column(name = "role_name", length = 64, nullable = false)
    private String roleName;

    /**
     * 권한 코드 (Foreign Key to permissions table)
     */
    @Column(name = "permission_code", length = 128, nullable = false)
    private String permissionCode;
}
