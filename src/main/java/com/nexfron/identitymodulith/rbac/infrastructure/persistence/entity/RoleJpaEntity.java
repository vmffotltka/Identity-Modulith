package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoleJpaEntity {

    @Id
    @Column(name = "name", length = 64)
    private String name; // 예: ADMIN, TEAM_LEADER

    @Column(name = "type", length = 32, nullable = false)
    private String type; // POSITION / CHANNEL / SKILL
}
