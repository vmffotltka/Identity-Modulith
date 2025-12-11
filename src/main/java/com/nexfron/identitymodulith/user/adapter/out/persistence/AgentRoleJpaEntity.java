package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.domain.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "agent_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentJpaEntity agent;

    @Column(nullable = false)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role.RoleType roleType;
}