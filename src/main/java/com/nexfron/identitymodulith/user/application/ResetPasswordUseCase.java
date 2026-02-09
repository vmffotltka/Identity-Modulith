package com.nexfron.identitymodulith.user.application;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public interface ResetPasswordUseCase {

    ResetPasswordResult resetPassword(String tenantId, UUID agentId, UUID actorId);

    @Getter
    @Builder
    class ResetPasswordResult {
        private final UUID agentId;
        private final String tempPassword;  // 일회성 임시 비밀번호
    }
}
