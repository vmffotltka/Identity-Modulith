package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionJpaEntity {

    @Id
    @Column(name = "code", length = 128)
    private String code; // 예: "user:manage", "org:view"
}
