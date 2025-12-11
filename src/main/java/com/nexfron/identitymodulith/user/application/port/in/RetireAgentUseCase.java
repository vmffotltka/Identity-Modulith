package com.nexfron.identitymodulith.user.application.port.in;

import java.util.UUID;

public interface RetireAgentUseCase {

    void retireAgent(UUID agentId);
}