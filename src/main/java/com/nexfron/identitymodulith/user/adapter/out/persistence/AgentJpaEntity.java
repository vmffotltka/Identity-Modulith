package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.domain.AgentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    @Column(nullable = false)
    private boolean passwordMustChange;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime retiredAt;

    @Column(columnDefinition = "TEXT")
    private String roles;  // JSON 형식: [{"name":"SUPERVISOR","type":"POSITION"}]

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}