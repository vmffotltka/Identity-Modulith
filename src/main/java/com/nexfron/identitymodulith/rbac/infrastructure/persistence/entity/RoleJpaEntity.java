package com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 역할(Role) JPA 엔티티
 *
 * 시스템에서 사용자에게 부여할 수 있는 역할을 정의합니다.
 * 예: ADMIN, TEAM_LEADER, MEMBER 등
 *
 * 각 역할은 다양한 권한(Permission)을 가질 수 있으며,
 * 역할이 변경되면 해당 역할을 가진 모든 사용자에게 영향을 줍니다.
 */
@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoleJpaEntity {

    /**
     * 역할명 (Primary Key)
     * 예: ADMIN, TEAM_LEADER, MEMBER
     */
    @Id
    @Column(name = "name", length = 64)
    private String name;

    /**
     * 역할의 타입 (POSITION / CHANNEL / SKILL)
     * - POSITION: 직급 관련 역할
     * - CHANNEL: 채널 관련 역할 (전화, 채팅 등)
     * - SKILL: 스킬 관련 역할
     */
    @Column(name = "type", length = 32, nullable = false)
    private String type;
}
