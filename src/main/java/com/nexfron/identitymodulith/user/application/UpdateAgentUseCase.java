package com.nexfron.identitymodulith.user.application;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public interface UpdateAgentUseCase {

    void updateAgent(UpdateAgentCommand command);

    void transferOrganization(UUID agentId, String newOrganizationId);

    @Getter
    @Builder
    class UpdateAgentCommand {
        private final UUID agentId;
        private final String name;
    }
}
