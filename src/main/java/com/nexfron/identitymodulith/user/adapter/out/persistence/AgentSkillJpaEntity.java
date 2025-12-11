package com.nexfron.identitymodulith.user.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "agent_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSkillJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentJpaEntity agent;

    @Column(nullable = false)
    private String skillName;
}