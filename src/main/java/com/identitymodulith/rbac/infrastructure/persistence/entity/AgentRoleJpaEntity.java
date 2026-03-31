package com.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** 에이전트-역할 매핑 엔티티. */
@Entity
@Table(
        name = "rbac_agent_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agent_roles",
                columnNames = {"agent_id", "role_id"}
        )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AgentRoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "agent_id", length = 36, nullable = false)
    private String agentId;

    @Column(name = "role_id", length = 36, nullable = false)
    private String roleId;

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}

