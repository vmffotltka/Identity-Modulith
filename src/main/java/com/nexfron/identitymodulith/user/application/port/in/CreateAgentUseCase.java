package com.nexfron.identitymodulith.user.application.port.in;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public interface CreateAgentUseCase {

    CreateAgentResult createAgent(CreateAgentCommand command);

    @Getter
    @Builder
    class CreateAgentCommand {
        private final String tenantId;
        private final String loginId;
        private final String name;
        private final String organizationId;
    }

    @Getter
    @Builder
    class CreateAgentResult {
        private final UUID agentId;
        private final String loginId;
        private final String tempPassword;  // 일회성 임시 비밀번호
    }
}