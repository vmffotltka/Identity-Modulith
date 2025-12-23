package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 권한(Permission) JPA 엔티티
 *
 * 시스템에서 정의할 수 있는 모든 권한을 나타냅니다.
 * 예: "user:manage", "org:view", "report:export" 등
 *
 * 권한은 역할(Role)에 할당되며, 역할이 사용자에게 부여됩니다.
 * 따라서 권한 변경은 신중하게 처리해야 합니다.
 */
@Entity
@Table(name = "permissions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionJpaEntity {

    /**
     * 권한 코드 (Primary Key)
     * 예: "user:manage", "org:view", "org:delete"
     * 형식: "domain:action"
     */
    @Id
    @Column(name = "code", length = 128)
    private String code;
}
