package com.nexfron.identitymodulith.user.application.port.in;

import com.nexfron.identitymodulith.user.domain.AgentStatus;
import com.nexfron.identitymodulith.user.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GetAgentUseCase {

    AgentInfo getAgent(UUID agentId);

    List<AgentInfo> getAgents(AgentSearchCriteria criteria);

    @Getter
    @Builder
    class AgentInfo {
        private final UUID id;
        private final String username;
        private final String name;
        private final UUID organizationId;
        private final AgentStatus status;
        private final boolean passwordMustChange;
        private final LocalDateTime createdAt;
        private final LocalDateTime retiredAt;
        private final Set<Role> roles;
        // 비밀번호는 절대 포함하지 않음
    }

    @Getter
    @Builder
    class AgentSearchCriteria {
        private final UUID organizationId;
        private final boolean includeRetired;  // 퇴사자 포함 여부
    }
}