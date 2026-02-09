package com.nexfron.identitymodulith.user.application;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public interface UpdateAgentUseCase {

    void updateAgent(UpdateAgentCommand command);

    void transferOrganization(String tenantId, UUID agentId, UUID actorId, String newOrganizationId);

    @Getter
    @Builder
    class UpdateAgentCommand {
        private final String tenantId;
        private final UUID agentId;
        private final UUID actorId;  // 수정을 요청한 사용자
        private final String name;
    }
}
