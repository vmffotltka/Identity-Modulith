package com.nexfron.identitymodulith.user.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AgentJpaEntity {

    @Id
    @Column(name = "agent_id", length = 36)
    private String agentId;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "login_id", length = 100, unique = true, nullable = false)
    private String loginId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "password_must_change")
    private Boolean passwordMustChange;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "sync_status", length = 20)
    private String syncStatus;

    @Column(name = "dept_id", length = 50)
    private String deptId;

    @Column(name = "role_id", length = 50)
    private String roleId;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
